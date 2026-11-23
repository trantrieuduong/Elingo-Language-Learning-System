import EmptyState from "../../../../components/EmptyState/EmptyState";
import ShadowingLessonLayout from "./ShadowingLessonLayout";

function ShadowingPageContent(props) {
  const { errorMessage, errorType, lesson, loading, onNavigate, segments, t } =
    props;

  if (loading) {
    return (
      <div className="shadowing-page-container">
        <div className="dictation-loading glass-card">
          <span className="material-symbols-outlined spinning">sync</span>
          <p>{t("shadowing.loading")}</p>
        </div>
      </div>
    );
  }

  if (errorType || !lesson) {
    const description =
      errorType === "premium"
        ? errorMessage
        : errorType === "not_found"
        ? t("shadowing.notFoundDesc")
        : errorMessage || t("shadowing.loadError");
    return (
      <div className="shadowing-page-container">
        <EmptyState
          icon="error"
          isError
          title={
            errorType === "not_found" ? t("shadowing.notFoundTitle") : undefined
          }
          description={description}
          actionLabel={t("lessonStudy.backToLessons")}
          actionVariant="primary"
          onAction={() => onNavigate && onNavigate("/lessons")}
        />
      </div>
    );
  }

  if (segments.length === 0) {
    return (
      <div className="shadowing-page-container">
        <EmptyState
          icon="layers_clear"
          description={t("lessonStudy.noSegments")}
          actionLabel={t("lessonStudy.backToLessons")}
          actionVariant="primary"
          onAction={() => onNavigate && onNavigate("/lessons")}
        />
      </div>
    );
  }

  return <ShadowingLessonLayout {...props} />;
}

export default ShadowingPageContent;
