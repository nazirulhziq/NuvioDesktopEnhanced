package com.nuvio.app.core.ui

// Atlas rows use the first country-code letter; columns use the second.
private val countries = "ad ae af ag ai al am ao aq ar as at au aw ax az ba bb bd be bf bg bh bi bj bl bm bn bo bq br bs bt bv bw by bz ca cc cd cf cg ch ci ck cl cm cn co cp cr cu cv cw cx cy cz de dg dj dk dm do dz ec ee eg eh er es et eu fi fj fk fm fo fr ga gb gd ge gf gg gh gi gl gm gn gp gq gr gs gt gu gw gy hk hm hn hr ht hu ic id ie il im in io iq ir is it je jm jo jp ke kg kh ki km kn kp kr kw ky kz la lb lc li lk lr ls lt lu lv ly ma mc md me mf mg mh mk ml mm mn mo mp mq mr ms mt mu mv mw mx my mz na nc ne nf ng ni nl no np nr nu nz om pa pe pf pg ph pk pl pm pn pr ps pt pw py qa re ro rs ru rw sa sb sc sd se sg sh si sj sk sl sm sn so sr ss st sv sx sy sz tc td tf tg th tj tk tl tm tn to tr tt tv tw tz ua ug um un us uy uz va vc ve vg vi vn vu wf ws xk ye yt za zm zw".split(' ').toSet()
internal fun flagColumn(code: String) = code[1] - 'a'
internal fun flagRow(code: String) = code[0] - 'a'

// Input is the player's normalized language key, not an arbitrary media title.
private val languageCountries = "en:gb ar:sa af:za am:et az:az be:by bn:bd bs:ba ca:es cs:cz da:dk el:gr et:ee eu:es fa:ir fil:ph ga:ie gl:es gu:in he:il hi:in hy:am ja:jp ka:ge kk:kz km:kh kn:in ko:kr lo:la ml:in mr:in ms:my my:mm ne:np nb:no nn:no pa:in si:lk sl:si sq:al sr:rs sv:se sw:ke ta:in te:in tl:ph uk:ua ur:pk vi:vn zh:cn cy:gb zu:za".split(' ').associate { it.substringBefore(':') to it.substringAfter(':') }
internal fun subtitleFlagCountry(key: String?): String? {
    val parts = key?.lowercase()?.replace('_', '-')?.split('-') ?: return null
    if (parts.first() in listOf("", "und")) return null
    val region = parts.drop(1).firstOrNull { it.length == 2 }
    return (region ?: languageCountries[parts.first()] ?: parts.first()).takeIf { it in countries }
}

internal data class FlagSpan(val start: Int, val end: Int, val country: String)
private val field = Regex("[^\\n|•]+")
private val code = Regex("[A-Z]{2}")
private val list = Regex("[A-Z]{2}(?:\\s*[/,]\\s*[A-Z]{2})*")
private val tagged = Regex("""^\s*(?:languages?|langs?|(?:dual |multi[ -]?)?audio|subs?|subtitles?)\s*[:/]\s*""", RegexOption.IGNORE_CASE)
private val bracket = Regex("""(?:^|\s)\[([A-Z]{2}(?:\s*[/,]\s*[A-Z]{2})*)\](?=\s|$)""")

internal fun streamFlagSpans(text: String): List<FlagSpan> = buildList {
    // UTF-16 representation of two regional indicators, portable across Kotlin targets.
    var i = 0
    fun indicator(at: Int) = text[at] == '\uD83C' && text[at + 1] in '\uDDE6'..'\uDDFF'
    while (i + 3 < text.length) {
        if (indicator(i) && indicator(i + 2)) {
            val country = "${'a' + (text[i + 1] - '\uDDE6')}${'a' + (text[i + 3] - '\uDDE6')}"
            if (country in countries) add(FlagSpan(i, i + 4, country))
            i += 4
        } else i++
    }
    fun addList(value: String, offset: Int) {
        if (!list.matches(value.trim())) return
        val tokens = code.findAll(value).toList()
        if (tokens.any { it.value.lowercase() !in countries }) return
        tokens.forEach { add(FlagSpan(offset + it.range.first, offset + it.range.last + 1, it.value.lowercase())) }
    }
    for (match in field.findAll(text)) {
        val value = match.value
        val offset = match.range.first
        val prefix = tagged.find(value)?.value.orEmpty()
        if (prefix.isNotEmpty() || '/' in value || ',' in value) addList(value.drop(prefix.length), offset + prefix.length)
        bracket.findAll(value).forEach {
            val group = it.groups[1]!!
            addList(group.value, offset + group.range.first)
        }
    }
}.sortedBy { it.start }
