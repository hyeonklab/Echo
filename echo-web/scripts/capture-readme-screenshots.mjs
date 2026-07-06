import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

import { chromium } from "playwright";

import { resolveScreenshotAuth } from "./readme-screenshot-auth.mjs";

const scriptsDir = path.dirname(fileURLToPath(import.meta.url));
const repoRoot = path.resolve(scriptsDir, "../..");
const outputDir = path.join(repoRoot, "docs", "screenshots");

const viewport = { width: 1440, height: 900 };

const screenshots = [
  { name: "login", path: "/login", requiresAuth: false },
  { name: "home", path: "/home", requiresAuth: true },
  { name: "friends", path: "/friends", requiresAuth: true },
  { name: "chat-list", path: "/chat", requiresAuth: true },
];

/** README 채팅방 스크린샷 (roomId, 파일명) */
const chatRoomScreenshots = [
  {
    name: "chat-room-link-preview",
    roomId: Number(process.env.SCREENSHOT_ROOM_LINK_PREVIEW ?? 27),
    readyAfterInput: ".flex-1.overflow-y-auto a[target='_blank'] p.font-semibold",
  },
  {
    name: "chat-room-image-preview",
    roomId: Number(process.env.SCREENSHOT_ROOM_IMAGE_PREVIEW ?? 32),
    readyAfterInput: ".flex-1.overflow-y-auto img.object-cover",
  },
];

/**
 * 페이지 로딩이 끝날 때까지 대기한다.
 */
async function waitForScreen(page) {
  await page.waitForLoadState("load");
  await page.waitForTimeout(1200);
}

/**
 * 컨텍스트에 JWT를 주입한다. (첫 navigation 전에 등록)
 */
async function injectAuthTokens(context, accessToken, refreshToken) {
  await context.addInitScript(
    ({ accessTokenValue, refreshTokenValue }) => {
      localStorage.setItem("accessToken", accessTokenValue);
      localStorage.setItem("refreshToken", refreshTokenValue);
    },
    {
      accessTokenValue: accessToken,
      refreshTokenValue: refreshToken,
    },
  );
}

/**
 * 인증이 필요한 화면이 준비될 때까지 대기한다.
 */
async function waitForAuthenticatedScreen(page, target) {
  if (!target.requiresAuth) {
    return;
  }

  const readySelectorByName = {
    home: "text=로그인됨",
    friends: "h1:has-text('친구')",
    "chat-list": "text=내 채팅방",
    "chat-room-link-preview": "input[placeholder='메시지를 입력하세요']",
    "chat-room-image-preview": "input[placeholder='메시지를 입력하세요']",
  };

  const selector = readySelectorByName[target.name];

  if (!selector) {
    return;
  }

  try {
    await page.locator(selector).first().waitFor({ state: "visible", timeout: 20000 });
  } catch {
    const bodyPreview = (await page.locator("body").innerText()).slice(0, 200);
    throw new Error(
      `${target.name} 화면 인증 대기 실패. CORS와 FRONTEND_URL/SCREENSHOT_BASE_URL을 확인해 주세요.\n${bodyPreview}`,
    );
  }
}

/**
 * 채팅방 메시지 영역을 맨 아래로 스크롤하고 미리보기 로딩을 기다린다.
 */
async function waitForChatRoomContent(page, target) {
  const messagesScroller = page.locator(".flex-1.overflow-y-auto").first();

  await messagesScroller.evaluate((element) => {
    element.scrollTop = element.scrollHeight;
  });

  if (!target.readyAfterInput) {
    return;
  }

  try {
    await page.locator(target.readyAfterInput).first().waitFor({ state: "visible", timeout: 25000 });
  } catch {
    const bodyPreview = (await page.locator("body").innerText()).slice(0, 300);
    throw new Error(
      `${target.name} 미리보기 로딩 실패 (roomId=${target.roomId}).\n${bodyPreview}`,
    );
  }

  await page.waitForTimeout(800);
}

/**
 * README용 스크린샷을 캡처한다.
 */
async function captureScreenshot(browser, baseUrl, target, accessToken, refreshToken) {
  const context = await browser.newContext({
    viewport,
    colorScheme: "dark",
    deviceScaleFactor: 1,
  });

  if (target.requiresAuth) {
    await injectAuthTokens(context, accessToken, refreshToken);
  }

  const page = await context.newPage();

  await page.goto(`${baseUrl}${target.path}`, { waitUntil: "domcontentloaded" });
  await waitForAuthenticatedScreen(page, target);

  if (target.roomId != null) {
    await waitForChatRoomContent(page, target);
  }

  await waitForScreen(page);

  const outputPath = path.join(outputDir, `${target.name}.png`);
  await page.screenshot({ path: outputPath, fullPage: false });
  await context.close();

  return outputPath;
}

async function main() {
  const { accessToken, refreshToken, baseUrl } = resolveScreenshotAuth();

  fs.mkdirSync(outputDir, { recursive: true });

  console.log(`screenshot baseUrl=${baseUrl}`);

  const browser = await chromium.launch({ headless: true });

  try {
    for (const target of screenshots) {
      const outputPath = await captureScreenshot(browser, baseUrl, target, accessToken, refreshToken);
      console.log(`saved ${outputPath}`);
    }

    for (const roomTarget of chatRoomScreenshots) {
      const outputPath = await captureScreenshot(
        browser,
        baseUrl,
        {
          name: roomTarget.name,
          path: `/chat/${roomTarget.roomId}`,
          requiresAuth: true,
          roomId: roomTarget.roomId,
          readyAfterInput: roomTarget.readyAfterInput,
        },
        accessToken,
        refreshToken,
      );
      console.log(`saved ${outputPath} (roomId=${roomTarget.roomId})`);
    }
  } finally {
    await browser.close();
  }
}

main().catch((error) => {
  console.error(error instanceof Error ? error.message : error);
  process.exit(1);
});
