package com.youxiang8727.googlenearbychatroom.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : UiState, E : UiEvent, F : UiEffect> : ViewModel() {

    abstract fun createInitialState(): S

    private val initialState: S by lazy { createInitialState() }

    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _event = MutableSharedFlow<E>()
    val event = _event.asSharedFlow()

    private val _effect = Channel<F>(Channel.BUFFERED)
    val effect = _effect.receiveAsFlow()

    init {
        subscribeEvents()
    }

    private fun subscribeEvents() {
        viewModelScope.launch {
            event.collect {
                handleEvent(it)
            }
        }
    }

    abstract fun handleEvent(event: E)

    fun setEvent(event: E) {
        viewModelScope.launch { _event.emit(event) }
    }

    protected fun setState(reduce: S.() -> S) {
        _uiState.update { it.reduce() }
    }

    protected fun setEffect(builder: () -> F) {
        val effectValue = builder()
        viewModelScope.launch { _effect.send(effectValue) }
    }
}
