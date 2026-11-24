// These flags are bundled bitmaps, not regional-indicator glyphs (unsupported by some desktop fonts).
const FLAG_COUNTRIES = new Set(('ad ae af ag ai al am ao aq ar as at au aw ax az ba bb bd be bf bg bh bi bj bl bm bn bo bq br bs bt bv bw by bz ca cc cd cf cg ch ci ck cl cm cn co cp cr cu cv cw cx cy cz de dg dj dk dm do dz ec ee eg eh er es et eu fi fj fk fm fo fr ga gb gd ge gf gg gh gi gl gm gn gp gq gr gs gt gu gw gy hk hm hn hr ht hu ic id ie il im in io iq ir is it je jm jo jp ke kg kh ki km kn kp kr kw ky kz la lb lc li lk lr ls lt lu lv ly ma mc md me mf mg mh mk ml mm mn mo mp mq mr ms mt mu mv mw mx my mz na nc ne nf ng ni nl no np nr nu nz om pa pe pf pg ph pk pl pm pn pr ps pt pw py qa re ro rs ru rw sa sb sc sd se sg sh si sj sk sl sm sn so sr ss st sv sx sy sz tc td tf tg th tj tk tl tm tn to tr tt tv tw tz ua ug um un us uy uz va vc ve vg vi vn vu wf ws xk ye yt za zm zw').split(' '));

const LANGUAGE_NAME_FLAGS = {
  english: "gb", british: "gb", american: "us", malay: "my", "bahasa melayu": "my", malaysian: "my",
  indonesian: "id", "bahasa indonesia": "id", chinese: "cn", mandarin: "cn", cantonese: "hk",
  japanese: "jp", korean: "kr", arabic: "sa", hindi: "in", tamil: "in", telugu: "in", malayalam: "in",
  spanish: "es", french: "fr", german: "de", italian: "it", portuguese: "pt", "brazilian portuguese": "br",
  russian: "ru", thai: "th", vietnamese: "vn", turkish: "tr", dutch: "nl", polish: "pl", swedish: "se",
  danish: "dk", norwegian: "no", finnish: "fi", hebrew: "il", ukrainian: "ua", czech: "cz", greek: "gr",
  romanian: "ro", hungarian: "hu", persian: "ir", farsi: "ir", bengali: "bd", filipino: "ph", tagalog: "ph",
  swahili: "ke", afrikaans: "za", serbian: "rs", croatian: "hr", bulgarian: "bg", slovak: "sk",
  slovenian: "si", lithuanian: "lt", latvian: "lv", estonian: "ee", catalan: "es", icelandic: "is",
};
const languageFlagCode = (key, label) => {
  const tag = String(key || '').trim().toLowerCase().replace(/_/g, '-');
  if (String(key || '').trim().startsWith('__')) return '';
  const named = name => Object.prototype.hasOwnProperty.call(LANGUAGE_NAME_FLAGS, name) ? LANGUAGE_NAME_FLAGS[name] : '';
  if (named(tag)) return named(tag);
  try {
    const locale = new Intl.Locale(tag);
    if (locale.language !== 'und') {
      const country = (locale.region || (locale.language === 'en' ? 'GB' : locale.maximize().region) || '').toLowerCase();
      if (FLAG_COUNTRIES.has(country)) return country;
    }
  } catch (_) { /* Providers sometimes supply a name or country rather than a language tag. */ }
  return named(String(label || '').trim().toLowerCase()) || (FLAG_COUNTRIES.has(tag) ? tag : '');
};

const makeLanguageFlag = code => {
  const icon = document.createElement('span');
  icon.className = 'language-flag';
  icon.dataset.country = code;
  icon.setAttribute('role', 'img');
  icon.setAttribute('aria-label', code.toUpperCase());
  // Atlas: 26 columns (second letter), 26 rows (first letter); tiles display at 20 x 15.
  icon.style.backgroundPosition = `${-20*(code.charCodeAt(1)-97)}px ${-15*(code.charCodeAt(0)-97)}px`;
  return icon;
};

const appendStreamFlagText = (node, value) => {
  const text = String(value || '');
  // Trust actual flag emoji anywhere. ASCII codes are ambiguous (IT, 12 GB, filenames),
  // so accept only explicit metadata: tagged fields, bracketed tags, or entire code lists.
  const token = /\p{Regional_Indicator}{2}|\b[A-Z]{2}\b/gu;
  const emojiCode = pair => [...pair].map(ch => String.fromCharCode(ch.codePointAt(0)-0x1f1e6+97)).join('');
  const isCodeList = field => /^[A-Z]{2}(?:\s*[/,]\s*[A-Z]{2})*$/.test(field.trim()) &&
    field.trim().split(/\s*[/,]\s*/).every(code => FLAG_COUNTRIES.has(code.toLowerCase()));
  const render = (segment, allowCodes) => {
    let pos = 0;
    for (const match of segment.matchAll(token)) {
      const word = match[0];
      const code = word.length === 2 ? (allowCodes && word.toLowerCase()) : emojiCode(word);
      if (!FLAG_COUNTRIES.has(code)) continue;
      node.append(segment.slice(pos,match.index), makeLanguageFlag(code));
      pos = match.index + word.length;
    }
    node.append(segment.slice(pos));
  };
  for (const field of text.split(/([\n|•])/)) {
    const tagged = field.match(/^(\s*(?:languages?|langs?|(?:dual |multi[ -]?)?audio|subs?|subtitles?)\s*[:/]\s*)(.*)$/i);
    if (tagged && isCodeList(tagged[2])) {
      node.append(tagged[1]); render(tagged[2], true);
    } else if (isCodeList(field) && /[/,]/.test(field)) {
      render(field, true);
    } else {
      let pos = 0;
      for (const m of field.matchAll(/(?:^|\s)\[[A-Z]{2}(?:\s*[/,]\s*[A-Z]{2})*\](?=\s|$)/g)) {
        if (!isCodeList(m[0].trim().slice(1, -1))) continue;
        render(field.slice(pos,m.index), false); render(m[0], true);
        pos = m.index + m[0].length;
      }
      render(field.slice(pos), false);
    }
  }
};
