import { useState } from 'react'
import './FlashCard.css'

export default function FlashCard({ card }) {
  const [isFlipped, setIsFlipped] = useState(false)

  if (!card) return null

  const handleFlip = () => {
    setIsFlipped(!isFlipped)
  }

  return (
    <div className="flashcard-wrapper" onClick={handleFlip}>
      <div className={`flashcard-inner ${isFlipped ? 'flipped' : ''}`}>
        {/* Mặt trước: Từ vựng */}
        <div className="flashcard-front glass-card">
          <h2 className="flashcard-term">{card.term}</h2>
          {card.pos && <span className="flashcard-pos badge-info">{card.pos}</span>}
          {card.image_url && (
             <img src={card.image_url} alt={card.term} className="flashcard-image" />
          )}
          <p className="flashcard-hint">Chạm để lật</p>
        </div>

        {/* Mặt sau: Định nghĩa, ví dụ */}
        <div className="flashcard-back glass-card">
          <h2 className="flashcard-term">{card.translation || card.term}</h2>{/* */}
          
          <div className="flashcard-details">
            {card.explanation_vi && (
              <div className="flashcard-section">
                <span className="material-symbols-outlined">menu_book</span>
                <p>{card.explanation_vi}</p>
              </div>
            )}
            
            {(card.examples_en || card.examples_vi) && (
              <div className="flashcard-section">
                <span className="material-symbols-outlined">record_voice_over</span>
                <div>
                  {card.examples_en && <p className="flashcard-example-en">"{card.examples_en}"</p>}
                  {card.examples_vi && <p className="flashcard-example-vi">{card.examples_vi}</p>}
                </div>
              </div>
            )}
          </div>
          
          <p className="flashcard-hint">Chạm để lật</p>
        </div>
      </div>
    </div>
  )
}
