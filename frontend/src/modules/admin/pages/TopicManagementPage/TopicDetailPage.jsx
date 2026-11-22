import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import PropTypes from 'prop-types';
import { useTranslation } from 'react-i18next';
import DataTable from '../../components/DataTable/DataTable';
import { AdminErrorState } from '../../components/Feedback/AdminFeedback';
import {
  BookOpenIcon,
  PlusIcon,
  EditIcon,
  TrashIcon,
  ArrowUpIcon,
  ArrowDownIcon,
  Volume2Icon,
  CloseIcon,
  EyeIcon,
} from '../../components/icons/AdminIcons';
import {
  fetchAdminDeckDetail,
  fetchDeckTopics,
  fetchTopicCards,
  reorderTopicCards,
  createSystemCard,
  updateSystemCard,
  deleteSystemCard,
} from '../../services/adminContentApi';
import { uploadFileForVerification } from '../../../../services/uploadFile';
import styles from './TopicDetailPage.module.css';

const isUkLocale = (loc) => {
  if (!loc) return false;
  const s = String(loc).toLowerCase().replace(/_/g, '-');
  return s === 'en-uk' || s === 'uk' || s.endsWith('-uk');
};

const isUsLocale = (loc) => {
  if (!loc) return false;
  const s = String(loc).toLowerCase().replace(/_/g, '-');
  return s === 'en-us' || s === 'us' || s.endsWith('-us');
};

export default function TopicDetailPage({ deckId, topicId, onNavigate }) {
  const { t } = useTranslation('admin');

  const [deck, setDeck] = useState(null);
  const [topic, setTopic] = useState(null);
  const [cards, setCards] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Modal create/edit card (SYSTEM only)
  const [cardModal, setCardModal] = useState({
    isOpen: false,
    mode: 'create', // 'create' | 'edit'
    cardId: null,
    term: '',
    pos: 'noun',
    translation: '',
    explanationVi: '',
    explanationEn: '',
    examplesVi: '',
    examplesEn: '',
    imageUrl: '',
    phoneticUk: '',
    audioUkUrl: '',
    phoneticUs: '',
    audioUsUrl: '',
    submitting: false,
    error: null,
  });

  // Modal delete card
  const [deleteCardModal, setDeleteCardModal] = useState({
    isOpen: false,
    card: null,
    submitting: false,
    error: null,
  });

  // Upload state & refs for card image & audio (UK and US)
  const [uploadingImage, setUploadingImage] = useState(false);
  const [uploadingAudioUk, setUploadingAudioUk] = useState(false);
  const [uploadingAudioUs, setUploadingAudioUs] = useState(false);
  const imageInputRef = useRef(null);
  const audioUkInputRef = useRef(null);
  const audioUsInputRef = useRef(null);

  // Load data
  const loadData = useCallback(async () => {
    if (!deckId || !topicId) return;
    try {
      setLoading(true);
      setError(null);
      const [deckData, topicsData, cardsData] = await Promise.all([
        fetchAdminDeckDetail(deckId),
        fetchDeckTopics(deckId),
        fetchTopicCards(topicId),
      ]);

      setDeck(deckData);
      const currentTopic = topicsData?.find((tp) => String(tp.id) === String(topicId));
      setTopic(currentTopic || { id: topicId, name: `Topic #${topicId}` });
      setCards(cardsData || []);
    } catch (err) {
      setError(err?.message || t('feedback.errorDesc'));
    } finally {
      setLoading(false);
    }
  }, [deckId, topicId, t]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const isUserDeck = deck?.ownerType === 'USER';

  // Play audio safely
  const handlePlayAudio = (url) => {
    if (!url) return;
    try {
      const audio = new Audio(url);
      audio.play().catch(() => {});
    } catch {
      // ignore audio play errors in sandbox / test
    }
  };

  // Card Modal Handlers
  const handleOpenCreateCard = () => {
    setCardModal({
      isOpen: true,
      mode: 'create',
      cardId: null,
      term: '',
      pos: 'noun',
      translation: '',
      explanationVi: '',
      explanationEn: '',
      examplesVi: '',
      examplesEn: '',
      imageUrl: '',
      phoneticUk: '',
      audioUkUrl: '',
      phoneticUs: '',
      audioUsUrl: '',
      submitting: false,
      error: null,
    });
  };

  const handleOpenEditCard = (card) => {
    const ukPhonetic = card.phonetics?.find((p) => isUkLocale(p.locale));
    const usPhonetic = card.phonetics?.find((p) => isUsLocale(p.locale));

    // Nếu không khớp rõ UK/US, dùng fallback phần tử đầu tiên vào UK
    const firstPhonetic = card.phonetics?.[0];
    const initialUkText = ukPhonetic?.text || (!usPhonetic && firstPhonetic ? firstPhonetic.text : '');
    const initialUkAudio = ukPhonetic?.audioUrl || (!usPhonetic && firstPhonetic ? firstPhonetic.audioUrl : '');

    setCardModal({
      isOpen: true,
      mode: 'edit',
      cardId: card.id,
      term: card.term || '',
      pos: card.pos || 'noun',
      translation: card.translation || '',
      explanationVi: card.explanationVi || '',
      explanationEn: card.explanationEn || '',
      examplesVi: card.examplesVi || '',
      examplesEn: card.examplesEn || '',
      imageUrl: card.imageUrl || '',
      phoneticUk: initialUkText || '',
      audioUkUrl: initialUkAudio || '',
      phoneticUs: usPhonetic?.text || '',
      audioUsUrl: usPhonetic?.audioUrl || '',
      submitting: false,
      error: null,
    });
  };

  const handleCloseCardModal = () => {
    setCardModal((prev) => ({ ...prev, isOpen: false, error: null }));
    if (imageInputRef.current) imageInputRef.current.value = '';
    if (audioUkInputRef.current) audioUkInputRef.current.value = '';
    if (audioUsInputRef.current) audioUsInputRef.current.value = '';
  };

  const handleImageFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('image/')) {
      setCardModal((prev) => ({
        ...prev,
        error: t('content.cards.uploadImageError'),
      }));
      return;
    }

    try {
      setUploadingImage(true);
      setCardModal((prev) => ({ ...prev, error: null }));
      const verifiedKey = await uploadFileForVerification(file, file.type || 'image/jpeg');
      setCardModal((prev) => ({ ...prev, imageUrl: verifiedKey }));
    } catch (err) {
      setCardModal((prev) => ({
        ...prev,
        error: err?.message || t('content.cards.uploadImageError'),
      }));
    } finally {
      setUploadingImage(false);
    }
  };

  const handleRemoveImage = () => {
    setCardModal((prev) => ({ ...prev, imageUrl: '' }));
    if (imageInputRef.current) imageInputRef.current.value = '';
  };

  // UK Audio Upload
  const handleAudioUkFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('audio/')) {
      setCardModal((prev) => ({
        ...prev,
        error: t('content.cards.uploadAudioError'),
      }));
      return;
    }

    try {
      setUploadingAudioUk(true);
      setCardModal((prev) => ({ ...prev, error: null }));
      const verifiedKey = await uploadFileForVerification(file, file.type || 'audio/mpeg');
      setCardModal((prev) => ({ ...prev, audioUkUrl: verifiedKey }));
    } catch (err) {
      setCardModal((prev) => ({
        ...prev,
        error: err?.message || t('content.cards.uploadAudioError'),
      }));
    } finally {
      setUploadingAudioUk(false);
    }
  };

  const handleRemoveAudioUk = () => {
    setCardModal((prev) => ({ ...prev, audioUkUrl: '' }));
    if (audioUkInputRef.current) audioUkInputRef.current.value = '';
  };

  // US Audio Upload
  const handleAudioUsFileChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (!file.type.startsWith('audio/')) {
      setCardModal((prev) => ({
        ...prev,
        error: t('content.cards.uploadAudioError'),
      }));
      return;
    }

    try {
      setUploadingAudioUs(true);
      setCardModal((prev) => ({ ...prev, error: null }));
      const verifiedKey = await uploadFileForVerification(file, file.type || 'audio/mpeg');
      setCardModal((prev) => ({ ...prev, audioUsUrl: verifiedKey }));
    } catch (err) {
      setCardModal((prev) => ({
        ...prev,
        error: err?.message || t('content.cards.uploadAudioError'),
      }));
    } finally {
      setUploadingAudioUs(false);
    }
  };

  const handleRemoveAudioUs = () => {
    setCardModal((prev) => ({ ...prev, audioUsUrl: '' }));
    if (audioUsInputRef.current) audioUsInputRef.current.value = '';
  };

  const handleSubmitCardModal = async (e) => {
    e.preventDefault();
    const {
      mode,
      cardId,
      term,
      pos,
      translation,
      explanationVi,
      explanationEn,
      examplesVi,
      examplesEn,
      imageUrl,
      phoneticUk,
      audioUkUrl,
      phoneticUs,
      audioUsUrl,
    } = cardModal;

    if (!term.trim() || !translation.trim()) return;

    try {
      setCardModal((prev) => ({ ...prev, submitting: true, error: null }));

      const phonetics = [];
      if (phoneticUk.trim() || audioUkUrl.trim()) {
        phonetics.push({
          text: phoneticUk.trim() || undefined,
          audioUrl: audioUkUrl.trim() || undefined,
          locale: 'en-UK',
        });
      }
      if (phoneticUs.trim() || audioUsUrl.trim()) {
        phonetics.push({
          text: phoneticUs.trim() || undefined,
          audioUrl: audioUsUrl.trim() || undefined,
          locale: 'en-US',
        });
      }

      const payload = {
        topicId: Number(topicId),
        term: term.trim(),
        pos: pos || undefined,
        translation: translation.trim(),
        explanationVi: explanationVi.trim() || undefined,
        explanationEn: explanationEn.trim() || undefined,
        examplesVi: examplesVi.trim() || undefined,
        examplesEn: examplesEn.trim() || undefined,
        imageUrl: imageUrl.trim() || undefined,
        phonetics: phonetics.length > 0 ? phonetics : undefined,
      };

      if (mode === 'create') {
        const created = await createSystemCard(payload);
        setCards((prev) => [...prev, created]);
      } else {
        const updated = await updateSystemCard(cardId, payload);
        setCards((prev) => prev.map((c) => (c.id === cardId ? updated : c)));
      }
      handleCloseCardModal();
    } catch (err) {
      setCardModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  // Delete Card
  const handleOpenDeleteCard = (card) => {
    setDeleteCardModal({
      isOpen: true,
      card,
      submitting: false,
      error: null,
    });
  };

  const handleConfirmDeleteCard = async () => {
    if (!deleteCardModal.card) return;
    try {
      setDeleteCardModal((prev) => ({ ...prev, submitting: true, error: null }));
      await deleteSystemCard(deleteCardModal.card.id);
      setCards((prev) => prev.filter((c) => c.id !== deleteCardModal.card.id));
      setDeleteCardModal({ isOpen: false, card: null, submitting: false, error: null });
    } catch (err) {
      setDeleteCardModal((prev) => ({
        ...prev,
        submitting: false,
        error: err?.response?.data?.message || err?.message || t('feedback.errorDesc'),
      }));
    }
  };

  // Reorder Cards
  const handleMoveCard = async (index, direction) => {
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    if (targetIndex < 0 || targetIndex >= cards.length) return;

    const newCards = [...cards];
    const temp = newCards[index];
    newCards[index] = newCards[targetIndex];
    newCards[targetIndex] = temp;

    setCards(newCards);

    try {
      const orderedIds = newCards.map((c) => c.id);
      await reorderTopicCards(topicId, orderedIds);
    } catch (err) {
      // Revert on error
      loadData();
    }
  };

  const columns = useMemo(
    () => [
      {
        title: '#',
        key: 'order',
        width: '60px',
        render: (_, __, index) => (
          <span style={{ fontWeight: 600, fontFamily: 'var(--adm-font-mono)' }}>
            #{index + 1}
          </span>
        ),
      },
      {
        title: t('content.cards.term'),
        key: 'term',
        width: '180px',
        render: (_, record) => (
          <div className={styles.termCell}>
            <span className={styles.termText}>{record.term}</span>
            {record.pos && <span className={styles.posBadge}>{record.pos}</span>}
          </div>
        ),
      },
      {
        title: t('content.cards.phonetics'),
        key: 'phonetics',
        width: '180px',
        render: (_, record) => (
          <div className={styles.phoneticsList}>
            {record.phonetics?.map((ph, idx) => (
              <div key={idx} className={styles.phoneticItem}>
                {ph.audioUrl && (
                  <button
                    type="button"
                    className={styles.audioBtn}
                    onClick={() => handlePlayAudio(ph.audioUrl)}
                    title={t('content.cards.playAudio')}
                    aria-label={`${t('content.cards.playAudio')} ${record.term}`}
                  >
                    <Volume2Icon size={12} />
                  </button>
                )}
                <span>{ph.text}</span>
                {ph.locale && <span className={styles.localeTag}>({ph.locale})</span>}
              </div>
            ))}
          </div>
        ),
      },
      {
        title: t('content.cards.translation'),
        dataIndex: 'translation',
        key: 'translation',
        width: '160px',
        render: (val) => <span className={styles.translationText}>{val}</span>,
      },
      {
        title: t('content.cards.explanation'),
        key: 'explanation',
        render: (_, record) => (
          <div className={styles.explanationBox}>
            {record.explanationVi && <div>{record.explanationVi}</div>}
            {record.explanationEn && (
              <div style={{ color: 'var(--adm-text-subtle)', fontStyle: 'italic' }}>
                {record.explanationEn}
              </div>
            )}
          </div>
        ),
      },
      {
        title: t('content.cards.example'),
        key: 'examples',
        render: (_, record) => (
          <div className={styles.exampleBox}>
            {record.examplesEn && <div className={styles.exampleEn}>{record.examplesEn}</div>}
            {record.examplesVi && <div className={styles.exampleVi}>{record.examplesVi}</div>}
          </div>
        ),
      },
      {
        title: t('content.cards.media'),
        key: 'media',
        width: '70px',
        render: (_, record) =>
          record.imageUrl ? (
            <img
              src={record.imageUrl}
              alt=""
              className={styles.cardThumbnail}
              onError={(e) => {
                e.target.style.display = 'none';
              }}
            />
          ) : (
            '—'
          ),
      },
      {
        title: t('content.decks.actions'),
        key: 'actions',
        width: '160px',
        align: 'right',
        render: (_, record, index) => {
          if (isUserDeck) {
            return (
              <div className={styles.actionsCell}>
                <span style={{ fontSize: 11, color: 'var(--adm-text-disabled)' }}>
                  Read-only
                </span>
              </div>
            );
          }

          return (
            <div className={styles.actionsCell}>
              <button
                type="button"
                className={styles.actionBtn}
                disabled={index === 0}
                onClick={() => handleMoveCard(index, 'up')}
                title={t('content.topics.moveUp')}
                aria-label={`${t('content.topics.moveUp')} ${record.term}`}
              >
                <ArrowUpIcon size={14} />
              </button>
              <button
                type="button"
                className={styles.actionBtn}
                disabled={index === cards.length - 1}
                onClick={() => handleMoveCard(index, 'down')}
                title={t('content.topics.moveDown')}
                aria-label={`${t('content.topics.moveDown')} ${record.term}`}
              >
                <ArrowDownIcon size={14} />
              </button>
              <button
                type="button"
                className={styles.actionBtn}
                onClick={() => handleOpenEditCard(record)}
                title={t('content.cards.editCardBtn')}
                aria-label={`${t('content.cards.editCardBtn')} ${record.term}`}
              >
                <EditIcon size={14} />
              </button>
              <button
                type="button"
                className={`${styles.actionBtn} ${styles.deleteActionBtn}`}
                onClick={() => handleOpenDeleteCard(record)}
                title={t('content.cards.deleteCardConfirm')}
                aria-label={`${t('content.cards.deleteCardConfirm')} ${record.term}`}
              >
                <TrashIcon size={14} />
              </button>
            </div>
          );
        },
      },
    ],
    [t, isUserDeck, cards.length]
  );

  if (error && !deck) {
    return (
      <div className={styles.pageContainer}>
        <AdminErrorState
          title={t('feedback.errorTitle')}
          description={error}
          onRetry={loadData}
        />
      </div>
    );
  }

  return (
    <div className={styles.pageContainer}>

      {/* Header Section */}
      <div className={styles.headerSection}>
        <div className={styles.titleArea}>
          <h1 className={styles.topicTitle}>{topic?.name}</h1>
          <div className={styles.topicMeta}>
            <span>
              {t('content.decks.deckName')}: <strong>{deck?.title}</strong>
            </span>
            <span>·</span>
            <span>{t('content.decks.cardsCount', { count: cards.length })}</span>
          </div>
        </div>
        {!isUserDeck && (
          <button
            type="button"
            className={styles.primaryBtn}
            onClick={handleOpenCreateCard}
          >
            <PlusIcon size={16} />
            {t('content.cards.createCardBtn')}
          </button>
        )}
      </div>

      {/* Cards Table */}
      <DataTable
        columns={columns}
        data={cards}
        keyField="id"
        isLoading={loading}
        emptyMessage={t('feedback.emptyList')}
      />

      {/* Create / Edit Card Modal */}
      {cardModal.isOpen && (
        <div className={styles.modalBackdrop} onClick={handleCloseCardModal}>
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>
                {cardModal.mode === 'create'
                  ? t('content.cards.createCardBtn')
                  : t('content.cards.editCardBtn')}
              </h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={handleCloseCardModal}
                aria-label={t('content.decks.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <form onSubmit={handleSubmitCardModal}>
              <div className={styles.modalBody}>
                {cardModal.error && (
                  <p className={styles.errorMessage}>{cardModal.error}</p>
                )}

                <div className={styles.formRow}>
                  <div className={styles.formGroup}>
                    <label htmlFor="cardTermInput" className={styles.formLabel}>
                      {t('content.cards.term')} *
                    </label>
                    <input
                      id="cardTermInput"
                      type="text"
                      className={styles.formInput}
                      value={cardModal.term}
                      onChange={(e) =>
                        setCardModal((prev) => ({ ...prev, term: e.target.value }))
                      }
                      required
                    />
                  </div>
                  <div className={styles.formGroup}>
                    <label htmlFor="cardPosInput" className={styles.formLabel}>
                      {t('content.cards.pos')}
                    </label>
                    <input
                      id="cardPosInput"
                      type="text"
                      className={styles.formInput}
                      placeholder="noun, verb, adj..."
                      value={cardModal.pos}
                      onChange={(e) =>
                        setCardModal((prev) => ({ ...prev, pos: e.target.value }))
                      }
                    />
                  </div>
                </div>

                <div className={styles.formGroup}>
                  <label htmlFor="cardTranslationInput" className={styles.formLabel}>
                    {t('content.cards.translation')} *
                  </label>
                  <input
                    id="cardTranslationInput"
                    type="text"
                    className={styles.formInput}
                    value={cardModal.translation}
                    onChange={(e) =>
                      setCardModal((prev) => ({ ...prev, translation: e.target.value }))
                    }
                    required
                  />
                </div>

                {/* Phonetics & Audio Section (en-UK & en-US) */}
                <div className={styles.phoneticsSection}>
                  <div className={styles.phoneticsSectionHeader}>
                    <span className={styles.phoneticsSectionTitle}>
                      {t('content.cards.phoneticSection')}
                    </span>
                    <span className={styles.optionalBadge}>{t('content.cards.optional')}</span>
                  </div>

                  {/* en-UK Row Card */}
                  <div className={styles.phoneticRowCard}>
                    <div className={styles.phoneticCardHeader}>
                      <span className={styles.phoneticBadgeUk}>en-UK</span>
                      <span>{t('content.cards.phoneticUk')}</span>
                    </div>

                    <div className={styles.phoneticInputsRow}>
                      <div className={styles.formGroup}>
                        <label className={styles.miniLabel}>{t('content.cards.phonetics')}</label>
                        <input
                          type="text"
                          className={styles.formInput}
                          placeholder="/ˈæp.əl/"
                          value={cardModal.phoneticUk}
                          onChange={(e) =>
                            setCardModal((prev) => ({ ...prev, phoneticUk: e.target.value }))
                          }
                        />
                      </div>

                      <div className={styles.formGroup}>
                        <label className={styles.miniLabel}>{t('content.cards.audioUk')}</label>
                        <input
                          type="file"
                          ref={audioUkInputRef}
                          accept="audio/*"
                          style={{ display: 'none' }}
                          onChange={handleAudioUkFileChange}
                        />
                        {cardModal.audioUkUrl ? (
                          <div className={styles.audioPlayerPill}>
                            <button
                              type="button"
                              className={styles.playAudioBtn}
                              onClick={() => handlePlayAudio(cardModal.audioUkUrl)}
                              title={t('content.cards.playAudio')}
                            >
                              <Volume2Icon size={14} />
                              <span>{t('content.cards.playAudioShort')}</span>
                            </button>
                            <button
                              type="button"
                              className={styles.fileActionBtn}
                              onClick={() => audioUkInputRef.current?.click()}
                              disabled={uploadingAudioUk}
                            >
                              {t('content.cards.changeAudioShort')}
                            </button>
                            <button
                              type="button"
                              className={`${styles.fileActionBtn} ${styles.fileRemoveBtn}`}
                              onClick={handleRemoveAudioUk}
                              disabled={uploadingAudioUk}
                            >
                              {t('content.cards.removeAudioShort')}
                            </button>
                          </div>
                        ) : (
                          <button
                            type="button"
                            className={styles.fileUploadBtn}
                            onClick={() => audioUkInputRef.current?.click()}
                            disabled={uploadingAudioUk}
                          >
                            {uploadingAudioUk
                              ? t('content.cards.uploadingAudio')
                              : t('content.cards.uploadAudio')}
                          </button>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* en-US Row Card */}
                  <div className={styles.phoneticRowCard}>
                    <div className={styles.phoneticCardHeader}>
                      <span className={styles.phoneticBadgeUs}>en-US</span>
                      <span>{t('content.cards.phoneticUs')}</span>
                    </div>

                    <div className={styles.phoneticInputsRow}>
                      <div className={styles.formGroup}>
                        <label className={styles.miniLabel}>{t('content.cards.phonetics')}</label>
                        <input
                          type="text"
                          className={styles.formInput}
                          placeholder="/ˈæp.əl/"
                          value={cardModal.phoneticUs}
                          onChange={(e) =>
                            setCardModal((prev) => ({ ...prev, phoneticUs: e.target.value }))
                          }
                        />
                      </div>

                      <div className={styles.formGroup}>
                        <label className={styles.miniLabel}>{t('content.cards.audioUs')}</label>
                        <input
                          type="file"
                          ref={audioUsInputRef}
                          accept="audio/*"
                          style={{ display: 'none' }}
                          onChange={handleAudioUsFileChange}
                        />
                        {cardModal.audioUsUrl ? (
                          <div className={styles.audioPlayerPill}>
                            <button
                              type="button"
                              className={styles.playAudioBtn}
                              onClick={() => handlePlayAudio(cardModal.audioUsUrl)}
                              title={t('content.cards.playAudio')}
                            >
                              <Volume2Icon size={14} />
                              <span>{t('content.cards.playAudioShort')}</span>
                            </button>
                            <button
                              type="button"
                              className={styles.fileActionBtn}
                              onClick={() => audioUsInputRef.current?.click()}
                              disabled={uploadingAudioUs}
                            >
                              {t('content.cards.changeAudioShort')}
                            </button>
                            <button
                              type="button"
                              className={`${styles.fileActionBtn} ${styles.fileRemoveBtn}`}
                              onClick={handleRemoveAudioUs}
                              disabled={uploadingAudioUs}
                            >
                              {t('content.cards.removeAudioShort')}
                            </button>
                          </div>
                        ) : (
                          <button
                            type="button"
                            className={styles.fileUploadBtn}
                            onClick={() => audioUsInputRef.current?.click()}
                            disabled={uploadingAudioUs}
                          >
                            {uploadingAudioUs
                              ? t('content.cards.uploadingAudio')
                              : t('content.cards.uploadAudio')}
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                </div>

                {/* Image Upload */}
                <div className={styles.formGroup}>
                  <label className={styles.formLabel}>{t('content.cards.imageUrl')}</label>
                  <input
                    type="file"
                    ref={imageInputRef}
                    accept="image/*"
                    style={{ display: 'none' }}
                    onChange={handleImageFileChange}
                  />
                  {cardModal.imageUrl ? (
                    <div className={styles.uploadedImageBox}>
                      <img
                        src={cardModal.imageUrl}
                        alt=""
                        className={styles.cardFormThumbnail}
                        onError={(e) => {
                          e.target.style.display = 'none';
                        }}
                      />
                      <div className={styles.imageActionButtons}>
                        <button
                          type="button"
                          className={styles.fileActionBtn}
                          onClick={() => imageInputRef.current?.click()}
                          disabled={uploadingImage}
                        >
                          {t('content.cards.changeImage')}
                        </button>
                        <button
                          type="button"
                          className={`${styles.fileActionBtn} ${styles.fileRemoveBtn}`}
                          onClick={handleRemoveImage}
                          disabled={uploadingImage}
                        >
                          {t('content.cards.removeImage')}
                        </button>
                      </div>
                    </div>
                  ) : (
                    <button
                      type="button"
                      className={styles.fileUploadBtn}
                      onClick={() => imageInputRef.current?.click()}
                      disabled={uploadingImage}
                    >
                      {uploadingImage
                        ? t('content.cards.uploadingImage')
                        : t('content.cards.uploadImage')}
                    </button>
                  )}
                  <span className={styles.fileHintText}>{t('content.cards.imageHint')}</span>
                </div>

                <div className={styles.formGroup}>
                  <label htmlFor="cardExpViInput" className={styles.formLabel}>
                    {t('content.cards.explanationVi')}
                  </label>
                  <textarea
                    id="cardExpViInput"
                    className={styles.formTextarea}
                    value={cardModal.explanationVi}
                    onChange={(e) =>
                      setCardModal((prev) => ({ ...prev, explanationVi: e.target.value }))
                    }
                  />
                </div>

                <div className={styles.formGroup}>
                  <label htmlFor="cardExEnInput" className={styles.formLabel}>
                    {t('content.cards.examplesEn')}
                  </label>
                  <input
                    id="cardExEnInput"
                    type="text"
                    className={styles.formInput}
                    value={cardModal.examplesEn}
                    onChange={(e) =>
                      setCardModal((prev) => ({ ...prev, examplesEn: e.target.value }))
                    }
                  />
                </div>

                <div className={styles.formGroup}>
                  <label htmlFor="cardExViInput" className={styles.formLabel}>
                    {t('content.cards.examplesVi')}
                  </label>
                  <input
                    id="cardExViInput"
                    type="text"
                    className={styles.formInput}
                    value={cardModal.examplesVi}
                    onChange={(e) =>
                      setCardModal((prev) => ({ ...prev, examplesVi: e.target.value }))
                    }
                  />
                </div>
              </div>

              <div className={styles.modalFooter}>
                <button
                  type="button"
                  className={styles.cancelBtn}
                  onClick={handleCloseCardModal}
                  disabled={
                    cardModal.submitting ||
                    uploadingImage ||
                    uploadingAudioUk ||
                    uploadingAudioUs
                  }
                >
                  {t('content.decks.cancelBtn')}
                </button>
                <button
                  type="submit"
                  className={styles.primaryBtn}
                  disabled={
                    cardModal.submitting ||
                    uploadingImage ||
                    uploadingAudioUk ||
                    uploadingAudioUs ||
                    !cardModal.term.trim() ||
                    !cardModal.translation.trim()
                  }
                >
                  {cardModal.mode === 'create'
                    ? t('content.decks.createBtn')
                    : t('content.decks.saveBtn')}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Card Modal */}
      {deleteCardModal.isOpen && (
        <div
          className={styles.modalBackdrop}
          onClick={() =>
            setDeleteCardModal({ isOpen: false, card: null, submitting: false, error: null })
          }
        >
          <div className={styles.modalCard} onClick={(e) => e.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h3 className={styles.modalTitle}>{t('content.cards.deleteCardConfirm')}</h3>
              <button
                type="button"
                className={styles.closeBtn}
                onClick={() =>
                  setDeleteCardModal({ isOpen: false, card: null, submitting: false, error: null })
                }
                aria-label={t('content.decks.cancelBtn')}
              >
                <CloseIcon size={18} />
              </button>
            </div>
            <div className={styles.modalBody}>
              {deleteCardModal.error && (
                <p className={styles.errorMessage}>{deleteCardModal.error}</p>
              )}
              <p style={{ margin: 0, fontSize: 13, color: 'var(--adm-text-main)' }}>
                {t('content.cards.deleteCardConfirm')}
              </p>
              {deleteCardModal.card && (
                <div
                  style={{
                    padding: 8,
                    borderRadius: 'var(--adm-radius-xs)',
                    background: 'var(--adm-bg-surface-subtle)',
                    fontWeight: 700,
                    fontSize: 14,
                  }}
                >
                  {deleteCardModal.card.term} - {deleteCardModal.card.translation}
                </div>
              )}
            </div>
            <div className={styles.modalFooter}>
              <button
                type="button"
                className={styles.cancelBtn}
                onClick={() =>
                  setDeleteCardModal({ isOpen: false, card: null, submitting: false, error: null })
                }
                disabled={deleteCardModal.submitting}
              >
                {t('content.decks.cancelBtn')}
              </button>
              <button
                type="button"
                className={styles.dangerBtn}
                onClick={handleConfirmDeleteCard}
                disabled={deleteCardModal.submitting}
              >
                {t('content.decks.confirmDeleteBtn')}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

TopicDetailPage.propTypes = {
  deckId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  topicId: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
  onNavigate: PropTypes.func,
};
