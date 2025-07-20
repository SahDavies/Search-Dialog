package com.studentofknowledge.JetSurvey.util

class GSuffArray(vararg inputTexts: String) {
    companion object {
        private const val CUTOFF = 5   // cutoff to insertion sort
    }

    private val texts: Array<CharArray>        // sentinel-appended input texts
    private val index: IntArray                // flattened suffix indices
    private val offsets: IntArray              // start position of each text in the flattened space
    private val m: Int                         // number of input texts
    private val totalChars: Int                // total number of chars (including sentinels)

    init {
        m = inputTexts.size
        texts = Array(m) { CharArray(0) }

        // 1) build sentinel-appended char arrays
        for (t in 0 until m) {
            texts[t] = (inputTexts[t] + '\u0000').toCharArray()
        }

        // 2) compute offsets and total character count
        offsets = IntArray(m)
        var cumulus = 0
        for (t in 0 until m) {
            offsets[t] = cumulus
            cumulus += texts[t].size
        }
        totalChars = cumulus

        // 3) allocate and fill the flat index: each entry = offsets[t] + position within text
        index = IntArray(totalChars)
        for (t in 0 until m) {
            val base = offsets[t]
            for (i in texts[t].indices) {
                index[base + i] = base + i
            }
        }

        // 4) sort the suffixes
        sort(0, totalChars - 1, 0)
    }

    // 3-way string quicksort on index[lo..hi], comparing starting at d-th character
    private fun sort(lo: Int, hi: Int, d: Int) {
        if (hi <= lo + CUTOFF) {
            insertion(lo, hi, d)
            return
        }
        var lt = lo
        var gt = hi
        val pivotCode = index[lo]
        val (pi, pj) = textId(pivotCode) to posInText(pivotCode)
        val v = texts[pi][pj + d]

        var i = lo + 1
        while (i <= gt) {
            val code = index[i]
            val (ti, tj) = textId(code) to posInText(code)
            val c = texts[ti][tj + d]
            when {
                c < v -> exch(lt++, i++)
                c > v -> exch(i, gt--)
                else -> i++
            }
        }

        sort(lo, lt - 1, d)
        if (v != '\u0000') sort(lt, gt, d + 1)
        sort(gt + 1, hi, d)
    }

    // insertion sort for small subarrays
    private fun insertion(lo: Int, hi: Int, d: Int) {
        for (i in lo..hi) {
            var j = i
            while (j > lo && less(index[j], index[j - 1], d)) {
                exch(j, j - 1)
                j--
            }
        }
    }

    // compare two suffixes starting at d
    private fun less(code1: Int, code2: Int, d: Int): Boolean {
        if (code1 == code2) return false
        val t1 = textId(code1)
        val t2 = textId(code2)
        var p1 = posInText(code1) + d
        var p2 = posInText(code2) + d

        while (p1 < texts[t1].size && p2 < texts[t2].size) {
            when {
                texts[t1][p1] < texts[t2][p2] -> return true
                texts[t1][p1] > texts[t2][p2] -> return false
            }
            p1++
            p2++
        }
        // if one runs out of chars, the longer original suffix is "greater"
        return code1 > code2
    }

    // swap two entries in the index
    private fun exch(i: Int, j: Int) {
        val tmp = index[i]
        index[i] = index[j]
        index[j] = tmp
    }

    // find which text a flat code belongs to via offsets[]
    private fun textId(code: Int): Int {
        // simple linear scan (m is usually small)
        for (t in m - 1 downTo 0) {
            if (code >= offsets[t]) return t
        }
        return 0
    }

    // compute position within its text
    private fun posInText(code: Int): Int {
        val t = textId(code)
        return code - offsets[t]
    }

    /**
     * Returns the i-th smallest suffix as a string.
     */
    fun select(i: Int): String {
        require(i in 0 until totalChars) { "Index out of bounds: $i" }
        val code = index[i]
        val t = textId(code)
        val p = posInText(code)
        return String(texts[t], p, texts[t].size - p)
    }

    /**
     * Returns the total number of suffixes.
     */
    fun length(): Int = totalChars

    /**
     * Returns the number of suffixes strictly less than the [query] string.
     * We note that `rank(select(i))` equals `i` for each `i`
     * between 0 and totalChars−1.
     * @param query the query string
     * @return the number of suffixes strictly less than [query]
     */
    fun rank(query: String): Int {
        var lo = 0
        var hi = totalChars - 1
        while (lo <= hi) {
            val mid = lo + (hi - lo) / 2
            val code = index[mid]
            val cmp = compareSuffixToQuery(code, query)
            if (cmp < 0) {
                // suffix < query → go right
                lo = mid + 1
            } else {
                // suffix ≥ query → go left
                hi = mid - 1
            }
        }
        return lo
    }

    // Compare the suffix encoded by `code` against `query`:
    //   <0 if suffix < query,
    //    0 if equal up through length(query),
    //   >0 if suffix > query
    private fun compareSuffixToQuery(code: Int, query: String): Int {
        val t = textId(code)
        val pos = posInText(code)
        var i = pos
        var j = 0
        val txt = texts[t]

        // walk both strings
        while (i < txt.size && j < query.length) {
            val c1 = txt[i]
            val c2 = query[j]
            if (c1 != c2) return c1 - c2
            i++
            j++
        }
        // if we ran out of the suffix (hit '\0'), or ran out of the query:
        return when {
            i == txt.size -> {
                // suffix ended ("\0") before or exactly at query's end → suffix ≤ query
                // but if query also ended, then equal up to query's length ⇒ return 0
                if (j == query.length) 0 else txt[i - 1] - query[j]
            }
            j == query.length -> {
                // query exhausted but suffix still has chars → suffix > query
                1
            }
            else -> 0 // unreachable
        }
    }

    /**
     * Given a query string, returns an array of original texts that contains the given string.
     */
    fun match(query: String): List<String> {
        // find the range of suffixes that start with 'query'
        val start = rank(query)
        // use a high "sentinel" to find the upper bound
        val hiQuery = query + '\uffff'
        val end = rank(hiQuery)

        val seen = BooleanArray(m)
        val result = mutableListOf<String>()

        // collect each distinct text ID in [start..end)
        for (i in start until end) {
            val code = index[i]
            val t = textId(code)
            if (!seen[t]) {
                seen[t] = true
                // rebuild the original string (drop the trailing '\0')
                val txt = texts[t]
                result.add(String(txt, 0, txt.size - 1))
            }
        }

        return result
    }

    fun index(query: String): List<Pair<Int, Int>> {
        // find the range of suffixes that start with 'query'
        val start = rank(query)
        // use a high "sentinel" to find the upper bound
        val hiQuery = query + '\uffff'
        val end = rank(hiQuery)

        return List(end-start) {
            (posInText(index[start+it])) to textId(index[start+it])
        }
    }
}

fun main() {
    // Unit test for GSuffArray index method
    val list = listOf("Hello world Hello", "Fellow", "Yellow", "Hero")
    val gsa = GSuffArray(*list.toTypedArray())
    val query = "ello"
    val positionsByItem = gsa.index(query = query)
        .groupBy { (_, itemIndex) -> itemIndex }
        .mapValues { (_, list) -> list.map { (start, _) -> start } }

    println(positionsByItem)

    println(gsa.index(query))
    list.mapIndexed { index, text ->
        println("$text:")
        println()
        // only style if there are matches for this item
        positionsByItem[index]?.forEach { start ->
            val end = (start + query.length)
            print(text.substring(startIndex = start, endIndex = end))
            print(", ")
        }
        println()
    }

}