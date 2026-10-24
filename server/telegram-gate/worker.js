/**
 * SOWZ Tracket · Telegram üzvlük qapısı (Cloudflare Worker)
 * ----------------------------------------------------------
 * Nə edir: tətbiq hər açılışda (və hər render-dən əvvəl) bu Worker-ə sorğu göndərir.
 * Worker Telegram Bot API ilə "bu adam @sowzfx kanalındadırmı?" deyə canlı yoxlayır.
 * Kanaldan çıxan/atılan adam BİR DƏFƏ belə keş olunmur — hər sorğu yenidən yoxlanır,
 * ona görə kanaldan çıxan üzv tətbiqi dərhal istifadə edə bilmir.
 *
 * Bot token YALNIZ burada (Worker secret) durur, APK-da heç vaxt görünmür.
 *
 * Qurulum (bir dəfə, Termux-dan və ya istənilən kompüterdən):
 *   npm install -g wrangler
 *   wrangler login
 *   wrangler secret put BOT_TOKEN        # @BotFather-dan aldığın token
 *   wrangler deploy
 *
 * Bot @sowzfx kanalında ADMIN olmalıdır (ən azı "Member-ləri gör" icazəsi ilə),
 * yoxsa getChatMember "Bad Request: member list is inaccessible" qaytarır.
 *
 * Çağırış: GET https://<worker-adın>.workers.dev/check?uid=123456789
 * Cavab:  {"ok":true,  "status":"member"}            -> icazə var
 *         {"ok":false, "reason":"not_member"}         -> kanalda deyil / çıxıb / atılıb
 *         {"ok":false, "reason":"bad_request"}        -> uid yanlış və ya bot admin deyil
 */

const CHANNEL_ID = "-1003471006837"; // @sowzfx
const OWNER_ID = "7501753341";       // Sowz — kanaldan asılı olmadan həmişə icazəli

const ALLOWED_STATUS = new Set(["creator", "administrator", "member", "restricted"]);
// "restricted" Telegram-da hələ üzvdür, sadəcə mesaj limiti var — icazəli sayırıq.
// "left" və "kicked" -> qadağan.

// Bu worker birdən çox tətbiqə xidmət edir. Yeni APK buraxanda bu rəqəmi
// (və lazım olsa download_url-u) əl ilə artır, sonra "Save and deploy" bas.
const VERSIONS = {
  sowz_tracket: { latest_version_code: 1, download_url: "https://t.me/sowzfx" },
  sow7rix: { latest_version_code: 4, download_url: "https://github.com/pubgcanavar/SOWZ-Files/releases/download/latest/Sow7Rix-Studio.apk" },
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (url.pathname === "/terms") return html(TERMS_HTML);
    if (url.pathname === "/privacy") return html(PRIVACY_HTML);

    if (url.pathname === "/version") {
      const app = (url.searchParams.get("app") || "").trim();
      const info = VERSIONS[app];
      if (!info) return json({ ok: false, reason: "unknown_app" }, 404);
      return json({ ok: true, ...info });
    }

    if (url.pathname === "/ai/status") {
      const configured = Boolean(env.ANTHROPIC_API_KEY && env.BOT_TOKEN);
      return json({
        ok: true,
        configured,
        reason: configured ? "ready" : (!env.ANTHROPIC_API_KEY ? "anthropic_key_missing" : "telegram_bot_token_missing")
      });
    }

    if (url.pathname === "/ai/chat") {
      if (request.method !== "POST") return json({ ok: false, reason: "method_not_allowed" }, 405);
      if (!env.ANTHROPIC_API_KEY) return json({ ok: false, reason: "ai_not_configured" }, 503);

      let input;
      try { input = await request.json(); }
      catch { return json({ ok: false, reason: "invalid_json" }, 400); }

      const uid = String(input?.uid || "").trim();
      if (!/^\d{3,15}$/.test(uid)) return json({ ok: false, reason: "bad_uid" }, 400);
      if (!env.BOT_TOKEN) return json({ ok: false, reason: "server_misconfigured" }, 500);

      // Re-check channel membership for every AI call. Bot token and provider key stay in Worker secrets.
      let memberResponse;
      try {
        const memberUrl = "https://api.telegram.org/bot" + env.BOT_TOKEN +
          "/getChatMember?chat_id=" + encodeURIComponent(CHANNEL_ID) +
          "&user_id=" + encodeURIComponent(uid);
        memberResponse = await fetch(memberUrl, { cf: { cacheTtl: 0 } });
      } catch {
        return json({ ok: false, reason: "telegram_unreachable" }, 502);
      }
      const memberData = await memberResponse.json().catch(() => null);
      if (!memberData?.ok) return json({ ok: false, reason: "membership_check_failed" }, 403);
      const memberStatus = memberData.result?.status;
      if (!memberStatus || !ALLOWED_STATUS.has(memberStatus)) {
        return json({ ok: false, reason: "not_member" }, 403);
      }

      if (!Array.isArray(input?.messages) || input.messages.length < 1) {
        return json({ ok: false, reason: "missing_messages" }, 400);
      }
      const messages = [];
      let totalChars = 0;
      for (const item of input.messages.slice(-12)) {
        if (!item || !["user", "assistant"].includes(item.role) || typeof item.content !== "string") {
          return json({ ok: false, reason: "invalid_message" }, 400);
        }
        const content = item.content.trim();
        if (!content || content.length > 2000) return json({ ok: false, reason: "message_too_long_or_empty" }, 400);
        totalChars += content.length;
        if (totalChars > 12000) return json({ ok: false, reason: "conversation_too_long" }, 413);
        messages.push({ role: item.role, content });
      }
      if (messages[messages.length - 1]?.role !== "user") {
        return json({ ok: false, reason: "last_message_must_be_user" }, 400);
      }

      let aiResponse;
      try {
        aiResponse = await fetch("https://api.anthropic.com/v1/messages", {
          method: "POST",
          headers: {
            "content-type": "application/json",
            "x-api-key": env.ANTHROPIC_API_KEY,
            "anthropic-version": "2026-10-21"
          },
          body: JSON.stringify({
            model: "claude-haiku-5-5",
            max_tokens: 900,
            system: "You are the Claude-powered assistant inside Sow7Rix Studio. Reply in Azerbaijani or Turkish according to the user's language. Be concise, practical, and helpful with the app and video-editing questions. Never ask for secrets or claim to have taken actions outside this chat.",
            messages
          })
        });
      } catch {
        return json({ ok: false, reason: "ai_provider_unreachable" }, 502);
      }

      const aiData = await aiResponse.json().catch(() => null);
      if (!aiResponse.ok || !aiData) {
        return json({ ok: false, reason: aiResponse.status === 429 ? "ai_rate_limited" : "ai_provider_error" },
          aiResponse.status === 429 ? 429 : 502);
      }
      const reply = Array.isArray(aiData.content)
        ? aiData.content.filter(part => part?.type === "text").map(part => part.text || "").join("\n").trim()
        : "";
      if (!reply) return json({ ok: false, reason: "empty_reply" }, 502);
      return json({ ok: true, model: aiData.model || "claude-haiku-5-5", reply });
    }

    if (url.pathname !== "/check") {
      return json({ ok: false, reason: "not_found" }, 404);
    }
    const uid = (url.searchParams.get("uid") || "").trim();
    if (!/^\d{3,15}$/.test(uid)) {
      return json({ ok: false, reason: "bad_uid" }, 400);
    }
    if (uid === OWNER_ID) {
      return json({ ok: true, status: "owner" });
    }
    if (!env.BOT_TOKEN) {
      return json({ ok: false, reason: "server_misconfigured" }, 500);
    }

    const tgUrl = "https://api.telegram.org/bot" + env.BOT_TOKEN + "/getChatMember?chat_id=" + encodeURIComponent(CHANNEL_ID) + "&user_id=" + encodeURIComponent(uid);
    let tgRes;
    try {
      tgRes = await fetch(tgUrl, { cf: { cacheTtl: 0 } }); // heç vaxt keşlənmir — həmişə canlı sorğu
    } catch (e) {
      return json({ ok: false, reason: "telegram_unreachable" }, 502);
    }
    const data = await tgRes.json().catch(() => null);
    if (!data) return json({ ok: false, reason: "bad_response" }, 502);

    if (!data.ok) {
      // Bot admin deyilsə, uid yanlışdırsa və s. Telegram "ok:false" qaytarır.
      return json({ ok: false, reason: "bad_request", detail: data.description || null }, 400);
    }

    const status = data.result && data.result.status;
    if (status && ALLOWED_STATUS.has(status)) {
      return json({ ok: true, status });
    }
    return json({ ok: false, reason: "not_member", status: status || "unknown" });
  },
};

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store", // browser/CDN tərəfində də keşlənməsin
    },
  });
}

function html(body) {
  return new Response(body, {
    headers: { "content-type": "text/html; charset=utf-8" },
  });
}

const PAGE_STYLE =
  "body{font-family:-apple-system,Segoe UI,Roboto,sans-serif;background:#0A0A0A;color:#F5F3FF;" +
  "max-width:680px;margin:0 auto;padding:32px 20px;line-height:1.6}" +
  "h1{color:#F2A318}h2{color:#F2A318;font-size:18px;margin-top:28px}" +
  "a{color:#F2A318}";

const TERMS_HTML =
  "<!doctype html><html><head><meta charset='utf-8'>" +
  "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
  "<title>Sow7Rix Studio - Kullanim Sartlari</title><style>" + PAGE_STYLE + "</style></head><body>" +
  "<h1>Kullanim Sartlari</h1>" +
  "<p>Bu sayfa Sow7Rix Studio uygulamasi (az.sow7rix.studio) icin kullanim sartlarini aciklar.</p>" +
  "<h2>1. Hizmet</h2><p>Sow7Rix Studio, kullanicilarin video icerigini TikTok'a yuklemeden once " +
  "isleyip (olcekleme, bitrate ve kare interpolasyonu) kendi TikTok hesaplariyla paylasmalarina " +
  "yardimci olan bir Android uygulamasidir.</p>" +
  "<h2>2. Hesap</h2><p>Uygulama, erisimi @sowzfx Telegram kanali uyeligiyle sinirlar. Kullanici " +
  "kanal uyeligini sona erdirirse uygulamaya erisimi de sona erer.</p>" +
  "<h2>3. Sorumluluk</h2><p>Yuklenen icerigin TikTok Topluluk Kurallarina uygunlugundan kullanici " +
  "sorumludur. Uygulama TikTok'un resmi web yukleme arayuzunu kullanir, TikTok hesaplarina " +
  "yetkisiz erisim saglamaz.</p>" +
  "<h2>4. Iletisim</h2><p>Sorular icin: <a href='https://t.me/sowzfx'>@sowzfx</a></p>" +
  "</body></html>";

const PRIVACY_HTML =
  "<!doctype html><html><head><meta charset='utf-8'>" +
  "<meta name='viewport' content='width=device-width, initial-scale=1'>" +
  "<title>Sow7Rix Studio - Gizlilik Politikasi</title><style>" + PAGE_STYLE + "</style></head><body>" +
  "<h1>Gizlilik Politikasi</h1>" +
  "<p>Sow7Rix Studio (az.sow7rix.studio) kullanicilarinin gizliligine onem verir.</p>" +
  "<h2>1. Toplanan Veri</h2><p>Uygulama, uyelik dogrulamasi icin girdiginiz Telegram kullanici " +
  "ID'sini, erisim kontrolu amaciyla sunucumuza (Cloudflare Worker) gonderir. Bu ID cihazinizda " +
  "yerel olarak saklanir, ucuncu taraflarla paylasilmaz.</p>" +
  "<h2>2. Video Icerigi</h2><p>Yuklemek icin sectiginiz videolar yalnizca cihazinizda islenir " +
  "(FFmpeg ile) ve dogrudan TikTok'un kendi resmi web sitesine gonderilir. Video dosyalari " +
  "bizim sunucularimizdan gecmez veya bizde saklanmaz.</p>" +
  "<h2>3. Ucuncu Taraf Servisler</h2><p>Uygulama TikTok'un kendi web sitesini (WebView icinde) ve " +
  "TikTok'un herkese acik oEmbed API'sini kullanir; bu servislerin kendi gizlilik politikalari " +
  "gecerlidir.</p>" +
  "<h2>4. Iletisim</h2><p>Sorular icin: <a href='https://t.me/sowzfx'>@sowzfx</a></p>" +
  "</body></html>";
