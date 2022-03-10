import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.subjects.PublishSubject
import org.junit.jupiter.api.Test
import java.lang.Thread.currentThread
import kotlin.concurrent.thread

class RxPlayground {

    @Test
    fun `base subject example`() {
        val subject = PublishSubject.create<String>()

        val testObserver = subject.map {
            log("inside map")
            "$it what"
        }.extendWithLogging()
            .test()

        subject.onNext("something")
        subject.onNext("anything")
        subject.onNext("sub")
        subject.onComplete()

        testObserver.await()
    }

    @Test
    fun `why onSubscribe is called on the main thread`() {
        val subject = PublishSubject.create<String>()

        val testObserver = subject.map {
            log("inside map")
            "$it what"
        }.subscribeOn(Schedulers.io())
            .extendWithLogging()
            .test()

        subject.onNext("something")
        subject.onNext("anything")
        subject.onNext("sub")
        subject.onComplete()

        testObserver.await()
    }

    @Test
    fun `actually off the main thread`() {
        val subject = PublishSubject.create<String>()

        val testObserver = subject.observeOn(Schedulers.io())
            .map {
                log("inside map")
                "$it what"
            }.extendWithLogging()
            .test()

        subject.onNext("something")
        subject.onNext("anything")
        subject.onNext("sub")
        subject.onComplete()

        testObserver.await()
    }

    @Test
    fun `map still on the main thread`() {
        val subject = PublishSubject.create<String>()

        val testObserver = subject.map {
            log("inside map")
            "$it what"
        }.observeOn(Schedulers.io())
            .extendWithLogging()
            .test()

        subject.onNext("something")
        subject.onNext("anything")
        subject.onNext("sub")
        subject.onComplete()

        testObserver.await()
    }

    @Test
    fun `something more complex`() {
        val stringSubject = PublishSubject.create<String>()
        val intSubject = PublishSubject.create<Int>()

        val testObserver = Observable.combineLatest(
            intSubject.observeOn(Schedulers.io()),
            stringSubject.observeOn(Schedulers.io())
        ) { string, int ->
            log("combining")
            string to int
        }.observeOn(Schedulers.newThread())
            .flatMap {
                log("mapping")
                Observable.just(Triple(it.first, it.second, "what"))
                    .subscribeOn(Schedulers.computation())
            }
            .extendWithLogging()
            .test()

        stringSubject.onNext("something")
        intSubject.onNext(1)
        stringSubject.onNext("anything")
        intSubject.onNext(2)
        intSubject.onNext(3)
        stringSubject.onComplete()
        intSubject.onComplete()

        testObserver.await()
    }

    @Test
    fun `wait why`() {
        val observable = Observable.create<String> { emitter ->
            thread(name = "Main thread", isDaemon = true) {
                listOf("something", "anything", "wtf").forEach { string ->
                    log("emitting $string")
                    emitter.onNext(string)
                }
                emitter.onComplete()
            }
        }

        observable
            .subscribeOn(Schedulers.io())
            .extendWithLogging()
            .test()
            .await()
    }

    private fun <T : Any> Observable<T>.extendWithLogging(): Observable<T> =
        this.doOnSubscribe {
            log("on Subscribe")
        }.doOnNext {
            log("on Next with $it")
        }.doOnComplete {
            log("on complete")
        }

    private fun log(message: String) {
        println("${currentThread().name} $message")
    }
}