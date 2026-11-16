import './Table.css';

/**
 * Table - Component bảng hiển thị dữ liệu
 *
 * @param {Array<{ key: string, title: React.ReactNode, width?: string|number, align?: 'left'|'center'|'right', render?: Function }>} columns - Cấu hình các cột (tiêu đề, độ rộng theo %, căn lề, render tùy biến)
 * @param {Array<Object>} data - Danh sách dữ liệu các hàng
 * @param {string|Function} [rowKey='id'] - Key định danh từng hàng
 * @param {string} [className=''] - Class CSS bổ sung
 */
function Table({ columns = [], data = [], rowKey = 'id', className = '' }) {
  const getRowKey = (row, index) => {
    if (typeof rowKey === 'function') {
      return rowKey(row, index);
    }
    return row?.[rowKey] ?? index;
  };

  const formatWidth = (width) => {
    if (!width) return undefined;
    return typeof width === 'number' ? `${width}%` : width;
  };

  return (
    <div className="table-wrapper">
      <table className={`table ${className}`.trim()}>
        <thead>
          <tr>
            {columns.map((col, index) => {
              const colKey = col.key ?? index;
              const width = formatWidth(col.width);
              const style = {
                ...(width ? { width } : {}),
                ...(col.align ? { textAlign: col.align } : {}),
              };

              return (
                <th key={colKey} style={Object.keys(style).length > 0 ? style : undefined}>
                  {col.title}
                </th>
              );
            })}
          </tr>
        </thead>
        <tbody>
          {data.map((row, rowIndex) => {
            const key = getRowKey(row, rowIndex);

            return (
              <tr key={key}>
                {columns.map((col, colIndex) => {
                  const cellKey = col.key ?? colIndex;
                  const value = col.key ? row?.[col.key] : undefined;
                  const content = col.render ? col.render(value, row, rowIndex) : (value ?? '—');
                  const style = col.align ? { textAlign: col.align } : undefined;

                  return (
                    <td key={cellKey} style={style}>
                      {content}
                    </td>
                  );
                })}
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

export default Table;
