package com.elingo.vocabulary.service.impl;

import com.elingo.common.exception.AppError;
import com.elingo.common.exception.AppException;
import com.elingo.vocabulary.dto.request.CreateCardPhoneticRequest;
import com.elingo.vocabulary.dto.request.CreateCardRequest;
import com.elingo.vocabulary.dto.request.UpdateCardRequest;
import com.elingo.vocabulary.dto.response.CardResponse;
import com.elingo.vocabulary.entity.*;
import com.elingo.vocabulary.mapper.CardMapper;
import com.elingo.vocabulary.repository.CardRepository;
import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.vocabulary.repository.TopicRepository;
import com.elingo.vocabulary.service.CardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j(topic = "CARD-SERVICE")
public class CardServiceImpl implements CardService {
    private final CardRepository cardRepository;
    private final TopicRepository topicRepository;
    private final DeckRepository deckRepository;
    private final CardMapper cardMapper;

    @Override
    @Transactional
    public CardResponse createUserCard(CreateCardRequest request, Long currentUserId) {
        Card card = insertCardCoreLogic(request, OwnerType.USER, currentUserId);
        return cardMapper.toCardResponse(card);
    }

    @Override
    @Transactional
    public CardResponse createSystemCard(CreateCardRequest request) {
        Card card = insertCardCoreLogic(request, OwnerType.SYSTEM, null);
        return cardMapper.toCardResponse(card);
    }

    private Card insertCardCoreLogic(CreateCardRequest request, OwnerType expectedOwnerType, Long currentUserId) {
        Topic topic = topicRepository.findById(request.topicId())
                .orElseThrow(() -> new AppException(AppError.TOPIC_NOT_FOUND));

        Deck deck = topic.getDeck();

        if (deck.getOwnerType() != expectedOwnerType) {
            throw new AppException(AppError.CARD_ACCESS_DENIED);
        }
        if (expectedOwnerType == OwnerType.USER && !deck.getOwner().getId().equals(currentUserId)) {
            throw new AppException(AppError.CARD_ACCESS_DENIED);
        }

        Card card = new Card();
        card.setDeck(deck);
        card.setTopic(topic);
        card.setOrder(cardRepository.findMaxOrderByTopicId(topic.getId()) + 1);
        card.setTerm(request.term());
        card.setPos(request.pos());
        card.setTranslation(request.translation());
        card.setExplanationVi(request.explanationVi());
        card.setExplanationEn(request.explanationEn());
        card.setExamplesVi(request.examplesVi());
        card.setExamplesEn(request.examplesEn());
        card.setImageUrl(request.imageUrl());

        if (request.phonetics() != null) {
            request.phonetics().forEach(p -> {
                CardPhonetic phonetic = new CardPhonetic();
                phonetic.setText(p.text());
                phonetic.setAudioUrl(p.audioUrl());
                phonetic.setLocale(p.locale());
                phonetic.setCard(card);
                card.getPhonetics().add(phonetic);
            });
        }
        cardRepository.save(card);

        deck.setCardCount(deck.getCardCount() + 1);
        deckRepository.save(deck);

        topic.setCardCount(topic.getCardCount() + 1);
        topicRepository.save(topic);
        return card;
    }

    @Override
    @Transactional
    public CardResponse updateUserCard(Long cardId, UpdateCardRequest request, Long currentUserId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));

        Deck deck = card.getDeck();
        if (deck.getOwnerType() != OwnerType.USER || !deck.getOwner().getId().equals(currentUserId)) {
            throw new AppException(AppError.CARD_ACCESS_DENIED);
        }
        return cardMapper.toCardResponse(updateCardCoreLogic(card, request));
    }

    @Override
    @Transactional
    public CardResponse updateSystemCard(Long cardId, UpdateCardRequest request) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));

        if (card.getDeck().getOwnerType() != OwnerType.SYSTEM) {
            throw new AppException(AppError.CARD_ACCESS_DENIED);
        }
        return cardMapper.toCardResponse(updateCardCoreLogic(card, request));
    }

    private Card updateCardCoreLogic(Card card, UpdateCardRequest request) {
        Integer requestedOrder = request.order();
        if (requestedOrder != null && !requestedOrder.equals(card.getOrder())) {
            Long topicId = card.getTopic().getId();
            Integer maxOrder = cardRepository.findMaxOrderByTopicId(topicId);
            if (requestedOrder < 0 || requestedOrder > maxOrder) {
                throw new AppException(AppError.CARD_ORDER_INVALID);
            }

            if (requestedOrder < card.getOrder()) {
                cardRepository.shiftOrdersDown(topicId, requestedOrder, card.getOrder());
            } else {
                cardRepository.shiftOrdersUp(topicId, card.getOrder(), requestedOrder);
            }
            card.setOrder(requestedOrder);
        }

        card.setTerm(request.term());
        card.setPos(request.pos());
        card.setTranslation(request.translation());
        card.setExplanationVi(request.explanationVi());
        card.setExplanationEn(request.explanationEn());
        card.setExamplesVi(request.examplesVi());
        card.setExamplesEn(request.examplesEn());
        card.setImageUrl(request.imageUrl());

        if (request.phonetics() != null) {
            updatePhonetics(card, request.phonetics());
        }
        return cardRepository.save(card);
    }

    private void updatePhonetics(
            Card card,
            List<CreateCardPhoneticRequest> requests
    ) {
        // Mỗi locale xác định một phonetic của card
        Map<String, CardPhonetic> existingByLocale =
                card.getPhonetics().stream()
                        .collect(Collectors.toMap(
                                CardPhonetic::getLocale,
                                Function.identity()
                        ));

        Set<String> requestedLocales = requests.stream()
                .map(CreateCardPhoneticRequest::locale)
                .collect(Collectors.toSet());

        // Không cho phép một locale xuất hiện nhiều lần
        if (requestedLocales.size() != requests.size()) {
            throw new AppException(AppError.DUPLICATE_CARD_PHONETIC_LOCALE);
        }

        // Xóa các phonetic không còn được gửi lên
        card.getPhonetics().removeIf(
                p -> !requestedLocales.contains(
                        p.getLocale()
                )
        );

        for (CreateCardPhoneticRequest request : requests) {
            String locale = request.locale();
            CardPhonetic phonetic = existingByLocale.get(locale);

            if (phonetic == null) {
                // Locale mới: tạo phonetic mới
                phonetic = new CardPhonetic();
                phonetic.setCard(card);
                card.getPhonetics().add(phonetic);
            }

            // Locale đã tồn tại: cập nhật bản ghi cũ
            phonetic.setText(request.text());
            phonetic.setAudioUrl(request.audioUrl());
            phonetic.setLocale(locale);
        }
    }

    @Override
    @Transactional
    public void deleteCard(Long cardId, Long currentUserId, boolean isAdmin) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new AppException(AppError.CARD_NOT_FOUND));

        Deck deck = card.getDeck();
        Topic topic = card.getTopic();

        if (!isAdmin) {
            if (deck.getOwnerType() != OwnerType.USER || !deck.getOwner().getId().equals(currentUserId)) {
                throw new AppException(AppError.CARD_ACCESS_DENIED);
            }
        } else {
            if (deck.getOwnerType() != OwnerType.SYSTEM) {
                throw new AppException(AppError.CARD_ACCESS_DENIED);
            }
        }

        deck.setCardCount(deck.getCardCount() - 1);
        deckRepository.save(deck);
        topic.setCardCount(topic.getCardCount() - 1);
        topicRepository.save(topic);

        if (deck.getPublishedAt() != null) {
            card.setIsDeleted(true);
            cardRepository.save(card);

            log.info("Card Soft-deleted successfully, cardId={}", cardId);
        } else {
            cardRepository.delete(card);

            log.info("Card Hard-deleted successfully, cardId={}", cardId);
        }

        cardRepository.compactOrdersAfterDelete(topic.getId(), card.getOrder());
    }

}
