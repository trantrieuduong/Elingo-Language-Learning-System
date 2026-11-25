import './Tabs.css';

/**
 * Tabs — Component chuyển đổi tab
 *
 * @param {Array<{ key: string, label: React.ReactNode, icon?: string }>} tabs - Danh sách tab
 * @param {string} activeTab - Key của tab đang được chọn
 * @param {Function} onChange - Callback (key: string) => void khi click chuyển tab
 * @param {string} [className=''] - Class CSS bổ sung
 */
function Tabs({ tabs = [], activeTab, onChange, className = '' }) {
  return (
    <nav className={`tabs-container glass-well ${className}`.trim()}>
      {tabs.map((tab) => {
        const isActive = tab.key === activeTab;

        return (
          <button
            key={tab.key}
            type="button"
            className={`tabs-btn ${isActive ? 'tabs-btn--active' : ''}`}
            onClick={() => {
              if (!isActive && onChange) {
                onChange(tab.key);
              }
            }}
          >
            {tab.icon && (
              <span
                className="material-symbols-outlined tabs-btn__icon"
                aria-hidden="true"
              >
                {tab.icon}
              </span>
            )}
            <span className="tabs-btn__label">{tab.label}</span>
          </button>
        );
      })}
    </nav>
  );
}

export default Tabs;
