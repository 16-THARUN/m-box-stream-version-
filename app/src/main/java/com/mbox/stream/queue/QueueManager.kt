package com.mbox.stream.queue

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mediaplayer.data.model.SourceTrack
import com.mbox.stream.radio.RadioFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.queueDataStore by preferencesDataStore(name = "mbox_stream_queue")

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

@Singleton
class QueueManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val TAG = "MBoxStream"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val gson = Gson()
    private val queueKey = stringPreferencesKey("saved_queue_json")
    private val positionKey = stringPreferencesKey("saved_position_json")

    private val _items = MutableStateFlow<List<QueueItem>>(emptyList())
    val items: StateFlow<List<QueueItem>> = _items.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _isShuffled = MutableStateFlow(false)
    val isShuffled: StateFlow<Boolean> = _isShuffled.asStateFlow()

    private val historyTrackIds = mutableListOf<String>()

    val currentItem: StateFlow<QueueItem?> = combine(_items, _currentIndex) { queue, index ->
        if (index in queue.indices) queue[index] else null
    }.stateIn(scope, SharingStarted.WhileSubscribed(5000), null)

    init {
        restoreQueue()
    }

    fun playNow(sourceTrack: SourceTrack) {
        val nowPlayingItem = QueueItem(
            sourceTrack = sourceTrack,
            section = QueueSection.NOW_PLAYING
        )
        val currentQueue = _items.value
        val upNextItems = currentQueue.filter { it.section == QueueSection.UP_NEXT }

        val newQueue = mutableListOf<QueueItem>()
        newQueue.add(nowPlayingItem)
        newQueue.addAll(upNextItems)

        _items.value = newQueue
        _currentIndex.value = 0
        addToHistory(sourceTrack.id)
        saveQueue()
    }

    fun playNext(sourceTrack: SourceTrack) {
        val newItem = QueueItem(sourceTrack = sourceTrack, section = QueueSection.UP_NEXT)
        val list = _items.value.toMutableList()
        val insertIndex = (_currentIndex.value + 1).coerceAtMost(list.size)
        list.add(insertIndex, newItem)
        _items.value = list
        saveQueue()
    }

    fun addToQueue(sourceTrack: SourceTrack) {
        val newItem = QueueItem(sourceTrack = sourceTrack, section = QueueSection.UP_NEXT)
        val list = _items.value.toMutableList()
        val radioIndex = list.indexOfFirst { it.section == QueueSection.RADIO }
        if (radioIndex != -1) {
            list.add(radioIndex, newItem)
        } else {
            list.add(newItem)
        }
        _items.value = list
        saveQueue()
    }

    fun addRadioItems(tracks: List<SourceTrack>) {
        val existingTracks = _items.value.map { it.sourceTrack }
        val filtered = RadioFilter.filterCandidateTracks(tracks, historyTrackIds, existingTracks)
        if (filtered.isEmpty()) return

        val radioItems = filtered.map {
            QueueItem(sourceTrack = it, section = QueueSection.RADIO)
        }

        val list = _items.value.toMutableList()
        list.addAll(radioItems)
        // Keep radio section under 30 items
        val currentRadioCount = list.count { it.section == QueueSection.RADIO }
        if (currentRadioCount > 30) {
            val excess = currentRadioCount - 30
            var removed = 0
            val iterator = list.iterator()
            while (iterator.hasNext() && removed < excess) {
                val item = iterator.next()
                if (item.section == QueueSection.RADIO && list.indexOf(item) > _currentIndex.value) {
                    iterator.remove()
                    removed++
                }
            }
        }
        _items.value = list
        saveQueue()
    }

    fun removeAt(index: Int) {
        val list = _items.value.toMutableList()
        if (index in list.indices && index != _currentIndex.value) {
            list.removeAt(index)
            if (index < _currentIndex.value) {
                _currentIndex.value -= 1
            }
            _items.value = list
            saveQueue()
        }
    }

    fun clearUpNext() {
        val currentQueue = _items.value
        val newQueue = currentQueue.filter { it.section != QueueSection.UP_NEXT }
        _items.value = newQueue
        if (_currentIndex.value >= newQueue.size) {
            _currentIndex.value = (newQueue.size - 1).coerceAtLeast(0)
        }
        saveQueue()
    }

    fun moveUpNextItem(fromIndex: Int, toIndex: Int) {
        val list = _items.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _items.value = list
            saveQueue()
        }
    }

    fun jumpTo(index: Int): QueueItem? {
        val list = _items.value
        if (index in list.indices) {
            _currentIndex.value = index
            val item = list[index]
            addToHistory(item.sourceTrack.id)
            saveQueue()
            return item
        }
        return null
    }

    fun next(): QueueItem? {
        val list = _items.value
        if (list.isEmpty()) return null

        val mode = _repeatMode.value
        if (mode == RepeatMode.ONE) {
            return list[_currentIndex.value]
        }

        var nextIdx = _currentIndex.value + 1
        if (nextIdx >= list.size) {
            if (mode == RepeatMode.ALL) {
                nextIdx = 0
            } else {
                return null
            }
        }

        _currentIndex.value = nextIdx
        val item = list[nextIdx]
        addToHistory(item.sourceTrack.id)
        saveQueue()
        return item
    }

    fun previous(): QueueItem? {
        val list = _items.value
        if (list.isEmpty()) return null

        var prevIdx = _currentIndex.value - 1
        if (prevIdx < 0) {
            prevIdx = if (_repeatMode.value == RepeatMode.ALL) list.size - 1 else 0
        }

        _currentIndex.value = prevIdx
        val item = list[prevIdx]
        addToHistory(item.sourceTrack.id)
        saveQueue()
        return item
    }

    fun toggleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleShuffle() {
        val newShuffle = !_isShuffled.value
        _isShuffled.value = newShuffle
        val list = _items.value.toMutableList()
        if (list.size > 1) {
            val cur = list.removeAt(_currentIndex.value)
            if (newShuffle) {
                list.shuffle()
            }
            list.add(0, cur)
            _items.value = list
            _currentIndex.value = 0
            saveQueue()
        }
    }

    private fun addToHistory(trackId: String) {
        historyTrackIds.add(trackId)
        if (historyTrackIds.size > 50) {
            historyTrackIds.removeAt(0)
        }
    }

    fun getHistoryIds(): List<String> = historyTrackIds.toList()

    private fun saveQueue() {
        scope.launch {
            try {
                val json = gson.toJson(_items.value)
                context.queueDataStore.edit { prefs ->
                    prefs[queueKey] = json
                    prefs[positionKey] = _currentIndex.value.toString()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving queue: ${e.message}")
            }
        }
    }

    private fun restoreQueue() {
        scope.launch {
            try {
                context.queueDataStore.data.collect { prefs ->
                    val json = prefs[queueKey]
                    val posStr = prefs[positionKey]
                    if (!json.isNullOrBlank()) {
                        val type = object : TypeToken<List<QueueItem>>() {}.type
                        val restored: List<QueueItem> = gson.fromJson(json, type)
                        _items.value = restored
                        _currentIndex.value = posStr?.toIntOrNull()?.coerceIn(0, (restored.size - 1).coerceAtLeast(0)) ?: 0
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring queue: ${e.message}")
            }
        }
    }
}
