import './FormAlert.css';

function FormAlert({ message = '' }) {
  if (!message) return null;

  return (
    <div className="form-alert form-alert--error">
      <span className="material-symbols-outlined">error</span>
      <span>{message}</span>
    </div>
  );
}

export default FormAlert;
