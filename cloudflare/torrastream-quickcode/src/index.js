const jsonHeaders = {
  "content-type": "application/json; charset=utf-8",
  "cache-control": "no-store, max-age=0",
  pragma: "no-cache",
  "x-content-type-options": "nosniff",
};

function reply(body, status = 200) {
  return new Response(JSON.stringify(body), { status, headers: jsonHeaders });
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/health") {
      return reply({ ok: true, version: 2 });
    }

    if (request.method !== "POST" || url.pathname !== "/v1/resolve") {
      return reply({ error: "Not found" }, 404);
    }

    const contentType = request.headers.get("content-type") || "";
    if (!contentType.toLowerCase().includes("application/json")) {
      return reply({ error: "Yêu cầu phải là JSON." }, 415);
    }

    const contentLength = Number(request.headers.get("content-length") || 0);
    if (contentLength > 2048) {
      return reply({ error: "Yêu cầu quá lớn." }, 413);
    }

    let input;
    try {
      input = await request.json();
    } catch {
      return reply({ error: "JSON không hợp lệ." }, 400);
    }

    const code = String(input.code || "").trim().toLowerCase();
    const provider = String(input.provider || "").trim().toLowerCase();
    if (!/^[a-z0-9_-]{2,64}$/.test(code) || !["torbox", "real_debrid"].includes(provider)) {
      return reply({ error: "Mã hoặc dịch vụ không hợp lệ." }, 400);
    }

    let config;
    try {
      config = JSON.parse(env.USER_CONFIG_JSON || "{}");
    } catch {
      return reply({ error: "Máy chủ chưa được cấu hình." }, 503);
    }

    if (Number(config.version) !== 2 || typeof config.users !== "object" || config.users === null) {
      return reply({ error: "Máy chủ chưa được cấu hình." }, 503);
    }

    const user = config.users[code];
    const key = user && typeof user === "object" ? String(user[provider] || "").trim() : "";
    if (key.length < 8 || key.startsWith("DAN_API_KEY_")) {
      // Cố ý dùng cùng một lỗi để không tiết lộ mã nào tồn tại.
      return reply({ error: "Mã hoặc dịch vụ không hợp lệ." }, 404);
    }

    return reply({
      version: 2,
      provider: provider === "torbox" ? "TorBox" : "RealDebrid",
      key,
    });
  },
};
