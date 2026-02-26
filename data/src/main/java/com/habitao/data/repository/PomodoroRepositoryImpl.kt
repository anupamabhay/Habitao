package com.habitao.data.repository

import com.habitao.data.local.dao.PomodoroSessionDao
import com.habitao.data.local.entity.PomodoroSessionEntity
import com.habitao.domain.model.PomodoroSession
import com.habitao.domain.repository.PomodoroRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PomodoroRepositoryImpl
    @Inject
    constructor(
        private val pomodoroSessionDao: PomodoroSessionDao,
        private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
    ) : PomodoroRepository {
        override suspend fun saveSession(session: PomodoroSession): Result<Unit> =
            withContext(dispatcher) {
                runCatching {
                    pomodoroSessionDao.insertSession(PomodoroSessionEntity.fromDomain(session))
                }
            }

        override suspend fun getSessionsForDate(date: LocalDate): Result<List<PomodoroSession>> =
            withContext(dispatcher) {
                runCatching {
                    val (startOfDay, endOfDay) = getDateRange(date)
                    pomodoroSessionDao.getSessionsForDateRange(startOfDay, endOfDay).map { it.toDomain() }
                }
            }

        override fun observeSessionsForDate(date: LocalDate): Flow<Result<List<PomodoroSession>>> {
            val (startOfDay, endOfDay) = getDateRange(date)
            return pomodoroSessionDao.observeSessionsForDateRange(startOfDay, endOfDay)
                .map { entities ->
                    runCatching { entities.map { it.toDomain() } }
                }
                .catch { e -> emit(Result.failure(e)) }
                .flowOn(dispatcher)
        }

        override fun observeSessionsForDateRange(startDate: LocalDate, endDate: LocalDate): Flow<Result<List<PomodoroSession>>> {
            val zoneId = ZoneId.systemDefault()
            val startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endMillis = endDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            return pomodoroSessionDao.observeSessionsForDateRange(startMillis, endMillis)
                .map { entities ->
                    runCatching { entities.map { it.toDomain() } }
                }
                .catch { e -> emit(Result.failure(e)) }
                .flowOn(dispatcher)
        }

        override suspend fun getTotalFocusTimeForDate(date: LocalDate): Result<Int> =
            withContext(dispatcher) {
                runCatching {
                    val (startOfDay, endOfDay) = getDateRange(date)
                    pomodoroSessionDao.getTotalFocusSeconds(startOfDay, endOfDay)
                }
            }

        private fun getDateRange(date: LocalDate): Pair<Long, Long> {
            val zoneId = ZoneId.systemDefault()
            val startOfDay = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
            return startOfDay to endOfDay
        }
    }
