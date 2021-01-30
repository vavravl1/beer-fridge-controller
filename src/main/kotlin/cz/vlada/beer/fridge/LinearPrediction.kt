package cz.vlada.beer.fridge

import cz.vlada.beer.fridge.repo.StoredValue
import org.apache.commons.math3.stat.regression.SimpleRegression
import java.time.Instant

object LinearPrediction {

    fun getValueAtTime(measured: List<StoredValue>, t: Instant): Float {
        val r = SimpleRegression()
        measured.forEach {
            r.addData(it.stored.asDouble(), it.value.toDouble())
        }
        return r.predict(t.asDouble()).toFloat()
    }

    private fun Instant.asDouble() = this.toEpochMilli().toDouble()
}
