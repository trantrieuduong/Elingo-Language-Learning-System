package com.elingo.battle.state;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public class LiveRound {

    private final int roundNo;
    private final LiveQuestion question;
    private final long startedAt;    
    private final long deadlineAt;   
    private final Map<Long, LiveAnswer> answers = new HashMap<>();
    private boolean ended;

    public LiveRound(int roundNo, LiveQuestion question, long startedAt, long deadlineAt) {
        this.roundNo = roundNo;
        this.question = question;
        this.startedAt = startedAt;
        this.deadlineAt = deadlineAt;
    }

    public boolean hasAnswered(Long userId) {
        return answers.containsKey(userId);
    }

    public LiveAnswer getAnswer(Long userId) {
        return answers.get(userId);
    }

    public void putAnswer(LiveAnswer a) {
        answers.put(a.userId(), a);
    }

    public int answerCount() {
        return answers.size();
    }

    public void markEnded() {
        ended = true;
    }
}

