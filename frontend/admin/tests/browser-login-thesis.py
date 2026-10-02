from pathlib import Path

from playwright.sync_api import sync_playwright


ARTIFACT_DIR = Path(__file__).resolve().parent / "artifacts"
ARTIFACT_DIR.mkdir(exist_ok=True)

with sync_playwright() as playwright:
    browser = playwright.chromium.launch(headless=True)
    page = browser.new_page(viewport={"width": 1680, "height": 944}, device_scale_factor=1)
    console_errors = []
    page.on("console", lambda message: console_errors.append(message.text) if message.type == "error" else None)
    page.goto("http://localhost:9527/login", wait_until="networkidle")
    page.locator(".login-worktable").wait_for(state="visible")
    page.screenshot(path=str(ARTIFACT_DIR / "login-thesis.png"), full_page=True)

    body_text = page.locator("body").inner_text()
    assert "花间集" in body_text
    assert "花店管理后台" in body_text
    assert "欢迎回来" not in body_text
    assert "Copyright" not in body_text
    assert "西安众邦网络科技有限公司" not in body_text

    card = page.locator(".login-worktable").bounding_box()
    assert card is not None
    assert 1240 <= card["width"] <= 1320
    assert 600 <= card["height"] <= 640
    assert page.locator(".flower-panel").is_visible()
    assert page.locator(".brand-panel").is_visible()
    assert page.locator(".form-panel").is_visible()
    assert page.get_by_label("用户名").is_visible()
    assert page.get_by_label("密码", exact=True).is_visible()
    assert page.get_by_role("button", name="登录").is_visible()
    assert not console_errors, f"browser console errors: {console_errors}"
    browser.close()

print(f"login screenshot: {ARTIFACT_DIR / 'login-thesis.png'}")
