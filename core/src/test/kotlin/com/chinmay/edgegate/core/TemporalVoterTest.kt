package com.chinmay.edgegate.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TemporalVoterTest {

    @Test fun confirmsAfterEnoughAgreeingFrames() {
        val v = TemporalVoter(minVotes = 3)
        assertNull(v.offer("KA01AB1234", 0.9f, 0))
        assertNull(v.offer("KA01AB1234", 0.9f, 100))
        val c = v.offer("KA01AB1234", 0.9f, 200)
        assertNotNull(c)
        assertEquals("KA01AB1234", c!!.plate)
        assertEquals(3, c.votes)
    }

    @Test fun outlierReadDoesNotWin() {
        val v = TemporalVoter(minVotes = 3)
        v.offer("KA01AB1234", 0.9f, 0)
        v.offer("KA01AB1284", 0.4f, 50) // one bad frame
        v.offer("KA01AB1234", 0.9f, 100)
        val c = v.offer("KA01AB1234", 0.9f, 150)
        assertEquals("KA01AB1234", c?.plate)
    }

    @Test fun doesNotReEmitWhileVehicleStaysInView() {
        val v = TemporalVoter(minVotes = 2, rearmMs = 1000)
        v.offer("MH12DE0045", 0.9f, 0)
        assertNotNull(v.offer("MH12DE0045", 0.9f, 100))
        for (t in 200L..900L step 100) assertNull(v.offer("MH12DE0045", 0.9f, t))
    }

    @Test fun reArmsAfterVehicleLeaves() {
        val v = TemporalVoter(minVotes = 2, rearmMs = 1000)
        v.offer("MH12DE0045", 0.9f, 0)
        assertNotNull(v.offer("MH12DE0045", 0.9f, 100))
        v.offer(null, 0f, 2500) // nothing in view for > rearmMs
        v.offer("MH12DE0045", 0.9f, 2600)
        assertNotNull(v.offer("MH12DE0045", 0.9f, 2700))
    }

    @Test fun oldReadsExpireFromWindow() {
        val v = TemporalVoter(windowMs = 500, minVotes = 3)
        v.offer("KA01AB1234", 0.9f, 0)
        v.offer("KA01AB1234", 0.9f, 100)
        assertNull(v.offer("KA01AB1234", 0.9f, 1000)) // first two have expired
    }
}
