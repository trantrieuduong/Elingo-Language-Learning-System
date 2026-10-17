package com.elingo.common.service.impl;

import com.elingo.vocabulary.repository.DeckRepository;
import com.elingo.common.service.SlugService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SlugServiceImpl implements SlugService {
    private final DeckRepository deckRepository;

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public static String generateSlug(String input) {
        if (input == null) return "";
        // Source - https://stackoverflow.com/a/1657250
        // Posted by McDowell, modified by community.
        // Retrieved 2026-10-10, License - CC BY-SA 2.5

        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");// Xóa dấu tiếng Việt/ký tự đặc biệt
        return slug.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String generateUniqueSlug(String title) {
        String baseSlug = generateSlug(title);
        if (baseSlug.isBlank()) {
            baseSlug = "slug";
        }
        if (!deckRepository.existsBySlug(baseSlug)) {
            return baseSlug;
        }

        int index = 1;
        String candidate = baseSlug + "-" + index;
        while (deckRepository.existsBySlug(candidate)) {
            index++;
            candidate = baseSlug + "-" + index;
        }
        return candidate;
    }
}