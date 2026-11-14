import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import QuestionArea from '@/modules/battle/components/QuestionArea/QuestionArea';
import { tr } from '../../../../utils/i18n';

describe('QuestionArea component', () => {
  const buildQuestion = (overrides = {}) => ({
    questionId: 1001,
    roundNo: 1,
    mode: 'MULTIPLE_CHOICE',
    term: 'apple',
    timeLimitMs: 15000,
    deadlineAt: Date.now() + 15000,
    options: [
      { id: 1, text: 'táo' },
      { id: 2, text: 'cam' },
      { id: 3, text: 'chuối' },
      { id: 4, text: 'nho' },
    ],
    ...overrides,
  });

  const buildTypingQuestion = (overrides = {}) =>
    buildQuestion({
      mode: 'TYPING',
      term: 'kem',
      options: undefined,
      answerPattern: [3, 5],
      firstChar: 'i',
      ...overrides,
    });

  describe('multiple choice', () => {
    it('renders the term and all options', () => {
      render(<QuestionArea question={buildQuestion()} />);

      expect(screen.getByRole('heading', { name: 'apple' })).toBeInTheDocument();
      expect(screen.getAllByRole('button')).toHaveLength(4);
      expect(screen.getByRole('button', { name: 'táo' })).toBeInTheDocument();
      expect(screen.getByRole('button', { name: 'nho' })).toBeInTheDocument();
    });

    it('submits the selected option and marks it as selected', async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();
      render(<QuestionArea question={buildQuestion()} onSubmit={onSubmit} />);

      await user.click(screen.getByRole('button', { name: 'cam' }));

      expect(onSubmit).toHaveBeenCalledTimes(1);
      expect(onSubmit).toHaveBeenCalledWith({ selectedOptionId: 2 });
      expect(screen.getByRole('button', { name: 'cam' })).toHaveClass('battle-option--selected');
    });

    it('does not throw when onSubmit is not provided', async () => {
      const user = userEvent.setup();
      render(<QuestionArea question={buildQuestion()} />);

      await user.click(screen.getByRole('button', { name: 'táo' }));

      expect(screen.getByRole('button', { name: 'táo' })).toHaveClass('battle-option--selected');
    });

    it('disables the options and shows the waiting message once answered', () => {
      render(<QuestionArea question={buildQuestion()} answered />);

      screen.getAllByRole('button').forEach((btn) => expect(btn).toBeDisabled());
      expect(screen.getByText(tr('battle:play.waitingOpponent'))).toBeInTheDocument();
    });

    it('does not show the waiting message when the round result is available', () => {
      const roundResult = { correctAnswer: 'táo', results: [] };
      render(<QuestionArea question={buildQuestion()} answered roundResult={roundResult} />);

      expect(screen.queryByText(tr('battle:play.waitingOpponent'))).not.toBeInTheDocument();
    });

    it('locks the options when the time is up', () => {
      render(<QuestionArea question={buildQuestion({ deadlineAt: Date.now() - 1000 })} />);

      screen.getAllByRole('button').forEach((btn) => expect(btn).toBeDisabled());
    });

    it('takes the server offset into account when computing the remaining time', () => {
      // Đồng hồ server nhanh hơn client 60s -> deadline theo server vẫn còn 10s
      const question = buildQuestion({ deadlineAt: Date.now() + 60000 + 10000, timeLimitMs: 10000 });
      render(<QuestionArea question={question} serverOffsetMs={60000} />);

      screen.getAllByRole('button').forEach((btn) => expect(btn).not.toBeDisabled());
    });

    it('does not submit when the options are locked', async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();
      render(<QuestionArea question={buildQuestion()} answered onSubmit={onSubmit} />);

      await user.click(screen.getByRole('button', { name: 'táo' }));

      expect(onSubmit).not.toHaveBeenCalled();
    });

    it('highlights the correct option and the wrong selected option after the round result', async () => {
      const user = userEvent.setup();
      const question = buildQuestion();
      const { rerender } = render(<QuestionArea question={question} onSubmit={vi.fn()} />);
      await user.click(screen.getByRole('button', { name: 'cam' }));

      rerender(
        <QuestionArea
          question={question}
          answered
          roundResult={{ correctAnswer: 'táo', results: [] }}
          myResult={{ answered: true, isCorrect: false, pointsGained: 0 }}
        />
      );

      expect(screen.getByRole('button', { name: /táo/ })).toHaveClass('battle-option--correct');
      expect(screen.getByRole('button', { name: /cam/ })).toHaveClass('battle-option--wrong');
      expect(screen.getByRole('button', { name: /chuối/ })).not.toHaveClass('battle-option--correct');
      expect(screen.getByRole('button', { name: /chuối/ })).not.toHaveClass('battle-option--wrong');
    });

    it('marks the selected option as correct when the answer is right', async () => {
      const user = userEvent.setup();
      const question = buildQuestion();
      const { rerender } = render(<QuestionArea question={question} onSubmit={vi.fn()} />);
      await user.click(screen.getByRole('button', { name: 'táo' }));

      rerender(
        <QuestionArea
          question={question}
          answered
          roundResult={{ correctAnswer: 'táo', results: [] }}
          myResult={{ answered: true, isCorrect: true, pointsGained: 140 }}
        />
      );

      expect(screen.getByRole('button', { name: /táo/ })).toHaveClass('battle-option--correct');
      expect(screen.getByRole('button', { name: /táo/ })).not.toHaveClass('battle-option--wrong');
    });

    it('renders no option when the question has no options', () => {
      render(<QuestionArea question={buildQuestion({ options: undefined })} />);

      expect(screen.queryAllByRole('button')).toHaveLength(0);
    });
  });

  describe('typing', () => {
    it('renders the input, the submit button and the hint', () => {
      const { container } = render(<QuestionArea question={buildTypingQuestion()} />);

      expect(screen.getByRole('heading', { name: 'kem' })).toBeInTheDocument();
      expect(screen.getByPlaceholderText(tr('battle:play.typeHere'))).toBeInTheDocument();
      expect(screen.getByRole('button', { name: tr('battle:play.submit') })).toBeDisabled();
      // Chữ cái đầu + gạch dưới theo độ dài từng từ
      expect(container.querySelector('.battle-typing-hint__text').textContent).toBe('i__  _____');
    });

    it('builds the hint without a first character', () => {
      const { container } = render(
        <QuestionArea question={buildTypingQuestion({ firstChar: undefined, answerPattern: [4, 2] })} />
      );

      expect(container.querySelector('.battle-typing-hint__text').textContent).toBe('____  __');
    });

    it('does not render the hint when there is no answer pattern', () => {
      const { container } = render(
        <QuestionArea question={buildTypingQuestion({ answerPattern: undefined })} />
      );

      expect(container.querySelector('.battle-typing-hint')).toBeNull();
    });

    it('submits the trimmed typed answer', async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();
      render(<QuestionArea question={buildTypingQuestion()} onSubmit={onSubmit} />);

      await user.type(screen.getByPlaceholderText(tr('battle:play.typeHere')), '  ice cream  ');
      await user.click(screen.getByRole('button', { name: tr('battle:play.submit') }));

      expect(onSubmit).toHaveBeenCalledWith({ typedAnswer: 'ice cream' });
    });

    it('submits when pressing Enter in the input', async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();
      render(<QuestionArea question={buildTypingQuestion()} onSubmit={onSubmit} />);

      await user.type(screen.getByPlaceholderText(tr('battle:play.typeHere')), 'ice cream{Enter}');

      expect(onSubmit).toHaveBeenCalledWith({ typedAnswer: 'ice cream' });
    });

    it('does not submit a blank answer', async () => {
      const user = userEvent.setup();
      const onSubmit = vi.fn();
      render(<QuestionArea question={buildTypingQuestion()} onSubmit={onSubmit} />);

      await user.type(screen.getByPlaceholderText(tr('battle:play.typeHere')), '   {Enter}');

      expect(onSubmit).not.toHaveBeenCalled();
      expect(screen.getByRole('button', { name: tr('battle:play.submit') })).toBeDisabled();
    });

    it('limits the answer to 100 characters', () => {
      render(<QuestionArea question={buildTypingQuestion()} />);

      expect(screen.getByPlaceholderText(tr('battle:play.typeHere'))).toHaveAttribute('maxlength', '100');
    });

    it('disables the input and shows the submitted label after answering', () => {
      render(<QuestionArea question={buildTypingQuestion()} answered />);

      expect(screen.getByPlaceholderText(tr('battle:play.typeHere'))).toBeDisabled();
      expect(screen.getByRole('button', { name: tr('battle:play.submitted') })).toBeDisabled();
      expect(screen.getByText(tr('battle:play.waitingOpponent'))).toBeInTheDocument();
    });

    it('disables the input when the time is up', () => {
      render(<QuestionArea question={buildTypingQuestion({ deadlineAt: Date.now() - 1000 })} />);

      expect(screen.getByPlaceholderText(tr('battle:play.typeHere'))).toBeDisabled();
    });
  });

  describe('timer bar', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('starts almost full and does not show the warning style', () => {
      const { container } = render(<QuestionArea question={buildQuestion({ deadlineAt: Date.now() + 15000 })} />);

      const bar = container.querySelector('.battle-timer-bar');
      expect(bar).toHaveStyle({ width: '100%' });
      expect(bar).not.toHaveClass('battle-timer-bar--warning');
    });

    it('shrinks over time and switches to the warning style below 30 percent', () => {
      const { container } = render(<QuestionArea question={buildQuestion({ deadlineAt: Date.now() + 15000 })} />);

      act(() => {
        vi.advanceTimersByTime(7500);
      });
      expect(container.querySelector('.battle-timer-bar')).toHaveStyle({ width: '50%' });
      expect(container.querySelector('.battle-timer-bar')).not.toHaveClass('battle-timer-bar--warning');

      act(() => {
        vi.advanceTimersByTime(5000);
      });
      expect(container.querySelector('.battle-timer-bar')).toHaveClass('battle-timer-bar--warning');
    });

    it('locks the options automatically when the timer reaches zero', () => {
      render(<QuestionArea question={buildQuestion({ deadlineAt: Date.now() + 1000, timeLimitMs: 1000 })} />);
      screen.getAllByRole('button').forEach((btn) => expect(btn).not.toBeDisabled());

      act(() => {
        vi.advanceTimersByTime(1200);
      });

      screen.getAllByRole('button').forEach((btn) => expect(btn).toBeDisabled());
    });

    it('stops the timer once the round result is shown and on unmount', () => {
      const question = buildQuestion();
      const { rerender, unmount } = render(<QuestionArea question={question} />);
      expect(vi.getTimerCount()).toBe(1);

      rerender(<QuestionArea question={question} roundResult={{ correctAnswer: 'táo' }} />);
      expect(vi.getTimerCount()).toBe(0);

      rerender(<QuestionArea question={question} />);
      expect(vi.getTimerCount()).toBe(1);
      unmount();
      expect(vi.getTimerCount()).toBe(0);
    });
  });

  describe('round result', () => {
    const roundResult = { correctAnswer: 'táo', results: [] };

    it('shows the correct answer and gained points when the player answered correctly', () => {
      render(
        <QuestionArea
          question={buildQuestion()}
          answered
          roundResult={roundResult}
          myResult={{ answered: true, isCorrect: true, pointsGained: 140 }}
        />
      );

      expect(screen.getByText(`${tr('battle:play.correctAnswer')}:`)).toBeInTheDocument();
      expect(screen.getByText(roundResult.correctAnswer, { selector: '.battle-round-result__correct-val' })).toBeInTheDocument();
      expect(screen.getByText(`+140 ${tr('battle:play.points')}`)).toBeInTheDocument();
    });

    it('shows zero points when the player answered incorrectly', () => {
      render(
        <QuestionArea
          question={buildQuestion()}
          answered
          roundResult={roundResult}
          myResult={{ answered: true, isCorrect: false, pointsGained: 0 }}
        />
      );

      expect(screen.getByText(tr('battle:play.zeroPoints'))).toBeInTheDocument();
    });

    it('shows time up when the player did not answer', () => {
      render(
        <QuestionArea
          question={buildQuestion()}
          roundResult={roundResult}
          myResult={{ answered: false, isCorrect: false, pointsGained: 0 }}
        />
      );

      expect(screen.getByText(tr('battle:play.timeUp'))).toBeInTheDocument();
    });

    it('shows time up when there is no result for the player', () => {
      render(<QuestionArea question={buildQuestion()} roundResult={roundResult} myResult={null} />);

      expect(screen.getByText(tr('battle:play.timeUp'))).toBeInTheDocument();
    });

    it('shows the correct typing answer in typing mode', () => {
      render(
        <QuestionArea
          question={buildTypingQuestion()}
          answered
          roundResult={{ correctAnswer: 'ice cream', results: [] }}
          myResult={{ answered: true, isCorrect: true, pointsGained: 120 }}
        />
      );

      expect(screen.getByText('ice cream', { selector: '.battle-round-result__correct-val' })).toBeInTheDocument();
      expect(screen.getByText(`+120 ${tr('battle:play.points')}`)).toBeInTheDocument();
    });
  });

  describe('edge cases', () => {
    it('renders an empty hint when the answer pattern is an empty list', () => {
      const { container } = render(<QuestionArea question={buildTypingQuestion({ answerPattern: [] })} />);

      expect(container.querySelector('.battle-typing-hint__text').textContent).toBe('');
    });

    it('does not submit a typed answer when the question is locked', async () => {
      const onSubmit = vi.fn();
      const { container } = render(
        <QuestionArea question={buildTypingQuestion()} answered onSubmit={onSubmit} />
      );

      fireEvent.submit(container.querySelector('form'));

      expect(onSubmit).not.toHaveBeenCalled();
    });

    it('does not submit when the typed answer is blank even if the form is submitted directly', () => {
      const onSubmit = vi.fn();
      const { container } = render(<QuestionArea question={buildTypingQuestion()} onSubmit={onSubmit} />);

      fireEvent.submit(container.querySelector('form'));

      expect(onSubmit).not.toHaveBeenCalled();
    });
  });
});
