package com.video.downloader.downloadvideo.videodownloader.core.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<State : UiState, Intent : UiIntent, Effect : UiEffect> : ViewModel() {

    abstract fun createInitialState(): State

    private val _uiState: MutableStateFlow<State> by lazy { MutableStateFlow(createInitialState()) }
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    private val _uiIntent: MutableSharedFlow<Intent> = MutableSharedFlow()
    val uiIntent: SharedFlow<Intent> = _uiIntent.asSharedFlow()

    private val _uiEffect: Channel<Effect> = Channel()
    val uiEffect = _uiEffect.receiveAsFlow()

    init {
        viewModelScope.launch {
            uiIntent.collect {
                handleIntent(it)
            }
        }
    }

    abstract fun handleIntent(intent: Intent)

    fun sendIntent(intent: Intent) {
        viewModelScope.launch {
            _uiIntent.emit(intent)
        }
    }

    protected fun setState(reduce: State.() -> State) {
        val newState = currentState.reduce()
        _uiState.value = newState
    }

    protected fun setEffect(builder: () -> Effect) {
        val effectValue = builder()
        viewModelScope.launch {
            _uiEffect.send(effectValue)
        }
    }

    val currentState: State
        get() = uiState.value
}
