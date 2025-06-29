package com.hexbit.rutmath.ui.game

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hexbit.rutmath.shared.game.EquationGenerator
import com.hexbit.rutmath.shared.model.Equation
import com.hexbit.rutmath.shared.model.Operation
import com.hexbit.rutmath.shared.usecase.DataUseCase
import kotlinx.coroutines.launch

/**
 * ViewModel for table multiplication games using shared KMP logic.
 * This replaces the old TableGameViewModel with shared business logic.
 */
class SharedTableGameViewModel(
    private val equationGenerator: EquationGenerator,
    private val dataUseCase: DataUseCase
) : ViewModel() {
    
    companion object {
        const val EXERCISES_COUNT = 20
        const val MAX_NUMBER = 100
    }
    
    private val _currentEquation = MutableLiveData<Equation>()
    val currentEquation: LiveData<Equation> = _currentEquation
    
    private val _gameProgress = MutableLiveData<String>()
    val gameProgress: LiveData<String> = _gameProgress
    
    private val _endGameEvent = MutableLiveData<Int>()
    val endGameEvent: LiveData<Int> = _endGameEvent
    
    private val _answerValidation = MutableLiveData<AnswerValidation>()
    val answerValidation: LiveData<AnswerValidation> = _answerValidation
    
    private val equations = mutableListOf<Pair<Equation, Boolean>>()
    private var currentEquationIndex = -1
    
    sealed class AnswerValidation {
        object Correct : AnswerValidation()
        object Incorrect : AnswerValidation()
    }
    
    /**
     * Starts a new table game.
     */
    fun startGame() {
        viewModelScope.launch {
            try {
                val generatedEquations = generateTableEquations()
                equations.clear()
                equations.addAll(generatedEquations.map { Pair(it, true) })
                currentEquationIndex = -1
                setNextActiveEquation()
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Validates user's answer.
     */
    fun validateAnswer(answer: Int) {
        val currentEq = _currentEquation.value ?: return
        val isCorrect = currentEq.correctAnswer == answer
        _answerValidation.value = if (isCorrect) AnswerValidation.Correct else AnswerValidation.Incorrect
    }
    
    /**
     * Proceeds to next equation or ends game.
     */
    fun proceedToNextQuestion(wasCorrect: Boolean = true) {
        if (currentEquationIndex >= 0 && !wasCorrect) {
            equations[currentEquationIndex] = equations[currentEquationIndex].copy(second = false)
        }
        setNextActiveEquation()
    }
    
    /**
     * Sets next active equation or triggers end game.
     */
    private fun setNextActiveEquation() {
        if (currentEquationIndex + 1 < equations.size) {
            currentEquationIndex++
            _currentEquation.value = equations[currentEquationIndex].first
            updateProgress()
        } else {
            // End game
            val rate = calculateGameRate()
            _endGameEvent.value = rate
        }
    }
    
    /**
     * Updates game progress display.
     */
    private fun updateProgress() {
        val progressText = "${currentEquationIndex + 1}/$EXERCISES_COUNT"
        _gameProgress.value = progressText
    }
    
    /**
     * Calculates final game rate (0-5 stars).
     */
    private fun calculateGameRate(): Int {
        val correctAnswers = equations.count { it.second }
        val percent = (correctAnswers.toFloat() / equations.size.toFloat()) * 100
        return when {
            percent <= 10 -> 0
            percent <= 29 -> 1
            percent <= 49 -> 2
            percent <= 69 -> 3
            percent <= 89 -> 4
            else -> 5
        }
    }
    
    /**
     * Generates equations for table multiplication game.
     */
    private fun generateTableEquations(): List<Equation> {
        val results = mutableListOf<Equation>()
        val operations = listOf(Operation.MULTIPLY, Operation.DIVIDE)
        
        while (results.size < EXERCISES_COUNT) {
            val operation = operations.random()
            val equation = equationGenerator.generateEquation(operation, MAX_NUMBER)
            
            // Ensure unique equations
            if (!results.contains(equation)) {
                results.add(equation)
            }
        }
        
        return results
    }
} 