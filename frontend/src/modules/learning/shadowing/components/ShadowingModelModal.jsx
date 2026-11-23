function ShadowingModelModal({
  chosenModelType,
  handleConfirmDownloadModel,
  setChosenModelType,
  setShowModelModal,
  showModelModal,
  t,
}) {
  if (!showModelModal) return null;
  const closeModal = () => setShowModelModal(false);
  const modelOptions = [
    { value: "standard", label: t("shadowing.modelStandard"), size: "73 MB" },
    { value: "light", label: t("shadowing.modelLight"), size: "39 MB" },
  ];
  return (
    <div className="shadowing-model-modal-overlay" onClick={closeModal}>
      <div
        className="shadowing-model-modal glass-capsule"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="shadowing-model-modal-header">
          <div className="shadowing-model-modal-title-group">
            <h2 className="shadowing-model-modal-title">
              {t("shadowing.askDownloadModelTitle")}
            </h2>
            <p className="shadowing-model-modal-desc">
              {t("shadowing.askDownloadModelDesc", {
                size: chosenModelType === "light" ? "39 MB" : "73 MB",
              })}
            </p>
          </div>
          <button type="button" className="btn-icon" onClick={closeModal}>
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>
        <div className="shadowing-model-options">
          {modelOptions.map(({ value, label, size }) => (
            <label
              key={value}
              className={`shadowing-model-radio-label glass-card ${
                chosenModelType === value
                  ? "shadowing-model-radio-label--active"
                  : ""
              }`}
            >
              <input
                type="radio"
                name="modelOption"
                value={value}
                checked={chosenModelType === value}
                onChange={() => setChosenModelType(value)}
              />
              <span className="shadowing-model-option-text">
                {label} (~{size})
              </span>
            </label>
          ))}
        </div>
        <div className="shadowing-model-modal-actions">
          <button type="button" className="btn-secondary" onClick={closeModal}>
            {t("common:cancel")}
          </button>
          <button
            type="button"
            className="btn-primary"
            onClick={handleConfirmDownloadModel}
          >
            {t("shadowing.btnDownload")}
          </button>
        </div>
      </div>
    </div>
  );
}

export default ShadowingModelModal;
