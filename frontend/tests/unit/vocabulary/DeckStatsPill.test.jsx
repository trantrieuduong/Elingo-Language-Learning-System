import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import DeckStatsPill from '@/modules/vocabulary/components/DeckStatsPill/DeckStatsPill'
import { LANGS, tr, useLang } from '../../utils/i18n'

describe('DeckStatsPill', () => {
  it('renders with default topicCount and cardCount (0, 0)', () => {
    render(<DeckStatsPill />)
    expect(
      screen.getByText(tr('learning:deckList.cardStats', { topicCount: 0, cardCount: 0 }))
    ).toBeInTheDocument()
  })

  it('applies custom className to root span', () => {
    const { container } = render(<DeckStatsPill topicCount={2} cardCount={5} className="custom-pill" />)
    const span = container.querySelector('.deck-stats-pill')
    expect(span).toHaveClass('custom-pill')
  })

  describe.each(LANGS)('renders localized i18n text (%s)', (lng) => {
    it('displays topicCount and cardCount', async () => {
      await useLang(lng)
      render(<DeckStatsPill topicCount={3} cardCount={10} />)
      expect(
        screen.getByText(tr('learning:deckList.cardStats', { topicCount: 3, cardCount: 10 }))
      ).toBeInTheDocument()
    })
  })
})
