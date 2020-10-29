package cz.vlada.beer.fridge

import cz.vlada.beer.fridge.repo.StoredValue
import java.time.Instant

object LinearPrediction {

    /*
    T = a*t + b // T...temperature, t...time
    T1 = a*t1 + b
    T2 = a*t2 + b
    (T1 - T2) = a * (t1 - t2) =>  a = (T1 - T2) / (t1 - t2)
    b = T1 - a*t1
    */
    fun getValueAtTime(
        pointA: StoredValue,
        pointB: StoredValue,
        t: Instant,
    ): Float {
        check(pointA.stored.isBefore(pointB.stored)) { "PointA must be taken before PointB." }
        val valueA = pointA.value.toFloat()
        val valueB = pointB.value.toFloat()
        val timeA = pointA.stored.epochSecond
        val timeB = pointB.stored.epochSecond

        val a = (valueA - valueB) / (timeA - timeB)
        val b = valueA - a * timeA

        return a * t.epochSecond + b
    }
}
