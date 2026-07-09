package io.github.toyota32k.dialog

import io.github.toyota32k.dialog.task.UtImmortalTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class UtImmortalTaskTest {
    private val testDispatcher = StandardTestDispatcher()

    // ユニークなタスク名を生成して、テスト間・並列タスク間での名前衝突を避ける
    private val nameSeq = AtomicInteger(0)
    private fun uniqueName() = "UtImmortalTaskTest.task-${nameSeq.incrementAndGet()}"

    @Before
    fun setup() {
        // UtImmortalTask は Dispatchers.Main 上で動作するため、テスト用ディスパッチャに差し替える。
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Test
    fun awaitTaskResult_returnsCallbackValue() = runTest(testDispatcher) {
        val result = UtImmortalTask.awaitTaskResult(uniqueName()) {
            123
        }
        assertEquals(123, result)
    }

    @Test
    fun awaitTaskResult_supportsNonPrimitiveValue() = runTest(testDispatcher) {
        val result = UtImmortalTask.awaitTaskResult {
            "hello"
        }
        assertEquals("hello", result)
    }

    @Test
    fun awaitTask_executesCallback() = runTest(testDispatcher) {
        var executed = false
        UtImmortalTask.awaitTask(uniqueName()) {
            executed = true
        }
        assertTrue(executed)
    }

    @Test
    fun launchTask_returnsJobThatRunsCallback() = runTest(testDispatcher) {
        var executed = false
        val job = UtImmortalTask.launchTask(uniqueName()) {
            executed = true
        }
        job.join()
        assertTrue(job.isCompleted)
        assertTrue(executed)
    }

    @Test
    fun awaitTaskResult_withDefault_returnsCallbackValueWhenNoError() = runTest(testDispatcher) {
        val result = UtImmortalTask.awaitTaskResultCatching(uniqueName(), default = -1) {
            42
        }
        assertEquals(42, result)
    }

    @Test
    fun awaitTaskResult_withDefault_returnsDefaultOnError() = runTest(testDispatcher) {
        val result = UtImmortalTask.awaitTaskResultCatching(uniqueName(),default = -1) {
            throw RuntimeException("boom")
        }
        assertEquals(-1, result)
    }

    @Test
    fun awaitTaskResult_rethrowsOnError() = runTest(testDispatcher) {
        var thrown: Throwable? = null
        try {
            UtImmortalTask.awaitTaskResult<Int>(uniqueName()) {
                throw IllegalArgumentException("bad")
            }
        } catch (e: Throwable) {
            thrown = e
        }
        assertTrue(thrown is IllegalArgumentException)
    }

//    @Test
//    fun taskName_isPreserved() {
//        val name = uniqueName()
//        val task = UtImmortalTask(name)
//        assertEquals(name, task.taskName)
//        assertEquals("UtImmortalTask($name)", task.toString())
//    }
//
//    @Test
//    fun isRunning_reflectsTaskLifecycle() = runTest(testDispatcher) {
//        val name = uniqueName()
//        val task = UtImmortalTask(name)
//        assertFalse(task.isRunning)
//        var runningInside = false
//        task.awaitTask {
//            runningInside = this.isRunning
//        }
//        assertTrue(runningInside)
//        // 完了後はタスクテーブルから外れ、実行中ではなくなる
//        assertFalse(task.isRunning)
//        assertNull(UtImmortalTaskManager.taskOf(name)?.task)
//    }

    @Test
    fun exclusiveTask_rejectsDuplicateConcurrentTask() = runTest(testDispatcher) {
        val name = uniqueName()
        val gate = CompletableDeferred<Unit>()

        val job1 = launch {
            UtImmortalTask.awaitTaskResult(name, allowSequential = false) {
                gate.await()
                1
            }
        }
        // job1 がロックを取得して gate 待ちで停止するところまで進める
        advanceUntilIdle()

        var thrown: Throwable? = null
        try {
            UtImmortalTask.awaitTaskResult(name, allowSequential = false) {
                2
            }
        } catch (e: Throwable) {
            thrown = e
        }
        assertTrue("duplicate exclusive task should fail", thrown is IllegalStateException)

        gate.complete(Unit)
        job1.join()
    }

    @Test
    fun sequentialTask_runsDuplicatesInOrder() = runTest(testDispatcher) {
        val name = uniqueName()
        val gate = CompletableDeferred<Unit>()
        val order = mutableListOf<Int>()

        val job1 = launch {
            UtImmortalTask.awaitTaskResult(name, allowSequential = true) {
                gate.await()
                order.add(1)
            }
        }
        // job1 がロックを取得して gate 待ちになるまで進める
        advanceUntilIdle()

        val job2 = launch {
            UtImmortalTask.awaitTaskResult(name, allowSequential = true) {
                order.add(2)
            }
        }
        // job2 は job1 の完了を待って順番に実行される
        advanceUntilIdle()
        assertEquals(emptyList<Int>(), order)

        gate.complete(Unit)
        job1.join()
        job2.join()

        assertEquals(listOf(1, 2), order)
    }

//    @Test
//    fun subTask_derivesNameFromParent() {
//        val parentName = uniqueName()
//        val parent = UtImmortalTask(parentName)
//        val sub = parent.subTask()
//        assertTrue(sub.taskName.startsWith("$parentName#"))
//    }

    @Test
    fun subTask_executesAndReturnsResult() = runTest(testDispatcher) {
        val result = UtImmortalTask.awaitTaskResult(uniqueName()) {
            subTask().awaitTaskResult {
                99
            }
        }
        assertEquals(99, result)
    }
}