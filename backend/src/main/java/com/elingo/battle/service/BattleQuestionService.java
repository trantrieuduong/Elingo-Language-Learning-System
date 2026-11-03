package com.elingo.battle.service;

import com.elingo.battle.entity.BattleMatch;
import com.elingo.battle.state.LiveQuestion;

import java.util.List;

public interface BattleQuestionService {
    List<LiveQuestion> createQuestions(BattleMatch match);
}

