package com.chinmay.edgegate

import android.app.Application
import android.content.Context
import com.chinmay.edgegate.ai.AnprPipeline
import com.chinmay.edgegate.ai.EngineMonitor
import com.chinmay.edgegate.ai.LiteRtPlateDetector
import com.chinmay.edgegate.ai.PlateOcr
import com.chinmay.edgegate.core.GateDecisionEngine
import com.chinmay.edgegate.core.TemporalVoter
import com.chinmay.edgegate.data.GateRepository
import com.chinmay.edgegate.data.db.AppDatabase
import com.chinmay.edgegate.util.Alerter
import com.chinmay.edgegate.util.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class EdgeGateApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.start()
    }
}

/** Manual dependency injection – small app, no need for Hilt. */
class AppContainer(private val context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings by lazy { Settings(context) }
    val repository by lazy { GateRepository(AppDatabase.create(context)) }
    val alerter by lazy { Alerter(context, settings) }
    val engineMonitor = EngineMonitor()
    val decisionEngine by lazy { GateDecisionEngine(cooldownMs = settings.current.cooldownSec * 1_000L) }

    fun start() {
        appScope.launch {
            // Keep the rules engine in sync with Settings.
            settings.state.map { it.cooldownSec }.distinctUntilChanged().collectLatest {
                decisionEngine.cooldownMs = it * 1_000L
            }
        }
        appScope.launch {
            // Privacy: apply the retention period on every launch.
            val days = settings.current.retentionDays
            repository.purgeOlderThan(System.currentTimeMillis() - days * 86_400_000L)
        }
    }

    /** Called on the camera analysis thread (see PlateAnalyzer). Picks up the latest settings. */
    fun createPipeline(): AnprPipeline {
        val s = settings.current
        return AnprPipeline(
            detector = LiteRtPlateDetector.createOrNull(context, preferGpu = s.preferGpu),
            ocr = PlateOcr(),
            voter = TemporalVoter(windowMs = 2_000, minVotes = s.votingFrames, minShare = 0.6f, rearmMs = 3_000),
        )
    }
}
