#!/usr/bin/env python3
"""DSHub - Play Store 에셋 생성기"""
from PIL import Image, ImageDraw, ImageFont
import os, math

OUT   = "/Users/junjonguk/Desktop/GITHUB/DSHub/store_assets"
RES   = "/Users/junjonguk/Desktop/GITHUB/DSHub/app/src/main/res"
FPATH = "/System/Library/Fonts/AppleSDGothicNeo.ttc"

for d in [f"{OUT}/phone", f"{OUT}/tablet7", f"{OUT}/tablet10"]:
    os.makedirs(d, exist_ok=True)

# ── 색상 ──────────────────────────────────────────────────────────────────
P_DK  = (13,  71, 161)   # #0D47A1
P     = (21, 101, 192)   # #1565C0
P_LT  = (30, 136, 229)   # #1E88E5
ACC   = (66, 165, 245)   # #42A5F5
BG    = (245, 247, 250)
SURF  = (255, 255, 255)
T1    = (30,  30,  30)
T2    = (120, 120, 120)
T3    = (200, 200, 200)
DIV   = (235, 235, 235)

STAGE_C = {
    "접수":    (158, 158, 158),
    "담당지정": (255, 152,   0),
    "처리중":  ( 33, 150, 243),
    "완료":    ( 76, 175,  80),
    "보류":    (255, 193,   7),
    "취소":    (244,  67,  54),
}

# ── 폰트 ──────────────────────────────────────────────────────────────────
_fc = {}
def F(sz, bold=False):
    k = (sz, bold)
    if k not in _fc:
        idx = 6 if bold else 3   # AppleSDGothicNeo: 3=Regular, 6=Bold
        try:
            _fc[k] = ImageFont.truetype(FPATH, sz, index=idx)
        except:
            _fc[k] = ImageFont.truetype(FPATH, sz, index=0)
    return _fc[k]

# ── 유틸 ──────────────────────────────────────────────────────────────────
def rr(d, x0, y0, x1, y1, r=16, fill=None, outline=None, lw=1):
    d.rounded_rectangle([x0,y0,x1,y1], radius=r, fill=fill, outline=outline, width=lw)

def shadow_card(d, x, y, w, h, r=24):
    rr(d, x+3, y+5, x+w+3, y+h+5, r=r, fill=(200,210,230))
    rr(d, x, y, x+w, y+h, r=r, fill=SURF)

def tc(d, text, x, y, w, f, fill):
    bb = d.textbbox((0,0), text, font=f)
    d.text((x+(w-(bb[2]-bb[0]))//2, y), text, font=f, fill=fill)

def clip(d, text, f, mw):
    for n in range(len(text), 0, -1):
        t = text[:n] + ("…" if n < len(text) else "")
        if d.textbbox((0,0), t, font=f)[2] <= mw:
            return t
    return "…"

def stage_pill(d, x, y, stage, sz=28):
    c = STAGE_C.get(stage, T2)
    bg = tuple(min(255, v+168) for v in c)
    f = F(sz, True)
    bb = d.textbbox((0,0), stage, font=f)
    tw, th = bb[2]-bb[0], bb[3]-bb[1]
    px, py = 20, 10
    W, H = tw+px*2, th+py*2
    rr(d, x, y, x+W, y+H, r=H//2, fill=bg)
    d.text((x+px, y+py), stage, font=f, fill=c)
    return W, H

def cat_chip(d, x, y, cat, sz=26):
    f = F(sz)
    bb = d.textbbox((0,0), cat, font=f)
    tw, th = bb[2]-bb[0], bb[3]-bb[1]
    px, py = 16, 8
    W, H = tw+px*2, th+py*2
    rr(d, x, y, x+W, y+H, r=10, fill=(224,240,253))
    d.text((x+px, y+py), cat, font=f, fill=P)
    return W, H

def gradient_rect(img, x0, y0, x1, y1, c_top, c_bot):
    d = ImageDraw.Draw(img)
    for i in range(y1-y0):
        t = i/(y1-y0)
        c = tuple(int(c_top[j]*(1-t)+c_bot[j]*t) for j in range(3))
        d.line([x0, y0+i, x1, y0+i], fill=c)

def gear(d, cx, cy, r_out, r_in, r_hole, teeth, fill, hole_fill):
    pts = []
    for i in range(teeth*2):
        a = math.radians(i*180/teeth - 90)
        r = r_out if i%2==0 else r_in
        pts.append((cx+r*math.cos(a), cy+r*math.sin(a)))
    d.polygon(pts, fill=fill)
    d.ellipse([cx-r_hole, cy-r_hole, cx+r_hole, cy+r_hole], fill=hole_fill)

# ── 공통 UI 컴포넌트 ───────────────────────────────────────────────────────
def statusbar(d, W, y=0, h=72, dark=False):
    fg = (255,255,255) if dark else T1
    d.text((50, y+18), "9:41", font=F(30, True), fill=fg)
    bx, by = W-56, y+h//2
    d.rectangle([bx-52, by-10, bx-16, by+10], outline=fg, width=2)
    d.rectangle([bx-50, by-8,  bx-24, by+8 ], fill=fg)
    d.rectangle([bx-16, by-5,  bx-14, by+5 ], fill=fg)
    for i,hh in enumerate([8,14,20]):
        d.rectangle([bx-110+i*18, by+10-hh, bx-100+i*18, by+10], fill=fg)

def appbar(d, W, title, y=72, h=144, back=False, acts=()):
    d.rectangle([0,y,W,y+h], fill=P)
    tx = 48
    if back:
        ax, ay = 64, y+h//2
        for dx, dy in [(-20,-18),(0,0),(-20,18)]:
            d.line([ax+dx, ay+dy, ax, ay], fill='white', width=5)
        tx = 136
    d.text((tx, y+(h-F(54,True).size)//2+4), title, font=F(54, True), fill='white')
    for i, act in enumerate(reversed(acts)):
        ix, iy = W-70-i*110, y+h//2
        if act == "refresh":
            d.arc([ix-22, iy-22, ix+22, iy+22], 40, 320, fill='white', width=5)
            d.polygon([(ix+16,iy-22),(ix+22,iy-8),(ix+8,iy-16)], fill='white')
        elif act == "settings":
            for a in range(0,360,45):
                r = math.radians(a)
                d.ellipse([ix+18*math.cos(r)-6, iy+18*math.sin(r)-6,
                           ix+18*math.cos(r)+6, iy+18*math.sin(r)+6], fill='white')
            d.ellipse([ix-12,iy-12,ix+12,iy+12], fill=P)
        elif act == "edit":
            d.polygon([(ix-18,iy+18),(ix+18,iy-18),(ix+22,iy-14),(ix-14,iy+22)], fill='white')
            d.line([ix-18,iy+18,ix-22,iy+22], fill='white', width=4)
        elif act == "delete":
            d.rectangle([ix-18,iy-14,ix+18,iy+18], outline='white', width=4)
            d.line([ix-24,iy-20,ix+24,iy-20], fill='white', width=4)
            d.line([ix-7,iy-14,ix-7,iy+14], fill='white', width=3)
            d.line([ix+7,iy-14,ix+7,iy+14], fill='white', width=3)

def searchbar(d, W, y, m=40):
    h = 90
    rr(d, m, y, W-m, y+h, r=45, fill=SURF, outline=DIV, lw=2)
    d.ellipse([m+42,y+25,m+74,y+57], outline=T3, width=4)
    d.line([m+70,y+53,m+84,y+67], fill=T3, width=4)
    d.text((m+100, y+24), "현장명, 담당자, 요청사항 검색", font=F(34), fill=T3)
    return h

def filterchips(d, W, y, sel="전체", m=40):
    stages = ["전체","접수","담당지정","처리중","완료","보류"]
    x, h = m, 68
    for s in stages:
        f = F(28, s==sel)
        bb = d.textbbox((0,0), s, font=f)
        w = bb[2]-bb[0]+40
        if x+w > W-m: break
        if s == sel:
            rr(d, x, y, x+w, y+h, r=34, fill=P)
            d.text((x+20, y+(h-28)//2), s, font=f, fill='white')
        else:
            rr(d, x, y, x+w, y+h, r=34, fill=SURF, outline=DIV, lw=2)
            d.text((x+20, y+(h-28)//2), s, font=F(28), fill=T2)
        x += w+14
    return h

def entrycard(d, W, y, e, m=40):
    cw, h = W-2*m, 224
    shadow_card(d, m, y, cw, h, r=22)
    px, py, cx, cy = 38, 28, m+38, y+28
    # 현장명 + 단계 배지
    site = clip(d, e["site"], F(40,True), cw-220)
    d.text((cx, cy), site, font=F(40,True), fill=T1)
    sw, sh = stage_pill(d, m+cw-px-200, y+py+4, e["stage"], 26)
    cy += 54
    # 구분 칩 + 담당자
    cw2, ch2 = cat_chip(d, cx, cy, e["cat"], 26)
    d.text((cx+cw2+18, cy+ch2//2-16), f"담당: {e['ass']}", font=F(30), fill=T2)
    cy += ch2+14
    # 요청사항 미리보기
    d.text((cx, cy), clip(d, e["req"], F(32), cw-px*2), font=F(32), fill=T2)
    cy += 46
    # 날짜 (오른쪽 정렬)
    dt = f"등록일: {e['date']}"
    bb = d.textbbox((0,0), dt, font=F(28))
    d.text((m+cw-px-(bb[2]-bb[0]), cy), dt, font=F(28), fill=T3)
    return h+22

def fab_btn(d, W, H):
    cx, cy, r = W-92, H-130, 80
    d.ellipse([cx-r//2+4, cy-r//2+5, cx+r//2+4, cy+r//2+5], fill=(190,200,220))
    d.ellipse([cx-r//2, cy-r//2, cx+r//2, cy+r//2], fill=P)
    d.line([cx-22, cy, cx+22, cy], fill='white', width=6)
    d.line([cx, cy-22, cx, cy+22], fill='white', width=6)

# ── 목업 데이터 ────────────────────────────────────────────────────────────
DATA = [
    {"site":"강남구청 서버실",     "stage":"처리중",  "cat":"기술지원", "ass":"김철수", "req":"LCD 모니터 2대 교체 긴급 요청",       "date":"2026-07-01"},
    {"site":"서울시청 2층 사무실", "stage":"완료",    "cat":"신규설치", "ass":"이영희", "req":"네트워크 장비 신규 설치 완료",         "date":"2026-06-28"},
    {"site":"종로구청 민원실",     "stage":"접수",    "cat":"장애처리", "ass":"박민준", "req":"프린터 출력 불량 – 업무 중단 상태",    "date":"2026-06-25"},
    {"site":"성동구청 IT실",       "stage":"담당지정","cat":"점검",     "ass":"최지영", "req":"월간 서버 정기 점검 일정 배정",        "date":"2026-06-20"},
    {"site":"마포구청 전산실",     "stage":"보류",    "cat":"이전설치", "ass":"정수현", "req":"서버 이전 설치 – 공사 지연으로 보류",  "date":"2026-06-15"},
]

# ── 화면 렌더러 ────────────────────────────────────────────────────────────
def scr_home(W, H, sel="전체"):
    img = Image.new("RGB", (W,H), BG)
    d = ImageDraw.Draw(img)
    y = 0
    statusbar(d, W, y, 72); y += 72
    appbar(d, W, "DS허브", y, 148, acts=("settings","refresh")); y += 148
    y += 20
    y += searchbar(d, W, y) + 20
    y += filterchips(d, W, y, sel) + 22
    d.line([0,y,W,y], fill=DIV, width=1); y += 18
    entries = [e for e in DATA if sel=="전체" or e["stage"]==sel]
    for e in entries:
        if y > H-260: break
        y += entrycard(d, W, y, e)
    fab_btn(d, W, H)
    return img

def scr_detail(W, H):
    img = Image.new("RGB", (W,H), BG)
    d = ImageDraw.Draw(img)
    e = DATA[0]
    y = 0
    statusbar(d, W, y, 72); y += 72
    appbar(d, W, e["site"][:10]+"…" if len(e["site"])>10 else e["site"],
           y, 148, back=True, acts=("delete","edit")); y += 148
    y += 24
    m, cw = 40, W-80

    # 상태 카드
    shadow_card(d, m, y, cw, 180, 22)
    d.text((m+40, y+26), "진행 상태", font=F(34), fill=T2)
    sw, sh = stage_pill(d, m+40, y+76, e["stage"])
    cat_chip(d, m+40+sw+24, y+80, e["cat"])
    y += 204

    # 현장 정보
    rows = [("현장명", e["site"]), ("담당자", "김철수")]
    ch = 56 + len(rows)*90 + 20
    shadow_card(d, m, y, cw, ch, 22)
    d.text((m+40, y+26), "현장 정보", font=F(38,True), fill=P)
    d.line([m+40,y+78,m+cw-40,y+78], fill=DIV, width=1)
    ry = y+92
    for lbl, val in rows:
        d.text((m+40, ry), lbl, font=F(32), fill=T2)
        d.text((m+250, ry), val, font=F(32,True), fill=T1)
        ry += 88
    # 지도 아이콘
    ix, iy2 = m+cw-70, y+92
    d.ellipse([ix-22,iy2-22,ix+22,iy2+22], fill=P)
    d.polygon([(ix,iy2+26),(ix-14,iy2+2),(ix+14,iy2+2)], fill=P)
    d.ellipse([ix-8,iy2-8,ix+8,iy2+8], fill='white')
    y += ch+20

    # 일정
    sched = [("등록일","2026-07-01"),("요청일","2026-07-01"),("예정일","2026-07-03"),("완료일","-")]
    ch2 = 56 + len(sched)*88 + 20
    shadow_card(d, m, y, cw, ch2, 22)
    d.text((m+40, y+26), "일정", font=F(38,True), fill=P)
    d.line([m+40,y+78,m+cw-40,y+78], fill=DIV, width=1)
    ry2 = y+92
    for lbl, val in sched:
        d.text((m+40, ry2), lbl, font=F(32), fill=T2)
        d.text((m+250, ry2), val, font=F(32,True), fill=T1 if val!="-" else T3)
        ry2 += 88
    y += ch2+20

    # 요청사항
    lines = ["LCD 모니터 2대 교체 요청.", "현재 화면 손상으로 업무 불가 상태입니다.", "긴급 처리 부탁드립니다."]
    ch3 = 56 + len(lines)*56 + 24
    shadow_card(d, m, y, cw, ch3, 22)
    d.text((m+40, y+26), "요청사항", font=F(38,True), fill=P)
    d.line([m+40,y+78,m+cw-40,y+78], fill=DIV, width=1)
    ry3 = y+92
    for line in lines:
        d.text((m+40, ry3), line, font=F(34), fill=T1)
        ry3 += 56
    return img

def scr_form(W, H):
    img = Image.new("RGB", (W,H), BG)
    d = ImageDraw.Draw(img)
    y = 0
    statusbar(d, W, y, 72); y += 72
    appbar(d, W, "기술지원 등록", y, 148, back=True); y += 148
    y += 24
    m, cw = 40, W-80

    def field(label, val, y, ico=None, disabled=False):
        lh = 38
        d.text((m, y), label, font=F(28), fill=T2)
        y += lh
        fh = 104
        fc = (248,249,250) if disabled else SURF
        rr(d, m, y, m+cw, y+fh, r=14, fill=fc, outline=DIV if disabled else (180,200,230), lw=2)
        vc = T2 if val else T3
        d.text((m+26, y+(fh-40)//2), val if val else f"{label} 입력", font=F(38), fill=vc)
        if ico == "cal":
            bx,by = m+cw-56, y+fh//2
            d.rectangle([bx-22,by-20,bx+22,by+20], outline=T2, width=3)
            d.line([bx-22,by-8,bx+22,by-8], fill=T2, width=2)
            for dx in [-10,10]:
                d.line([bx+dx,by-24,bx+dx,by-14], fill=T2, width=3)
        elif ico == "drop":
            bx,by = m+cw-52, y+fh//2
            d.polygon([(bx-18,by-8),(bx+18,by-8),(bx,by+12)], fill=T2)
        return lh + fh + 18

    def section(title, y):
        d.text((m, y), title, font=F(36,True), fill=P)
        return y + 54

    y = section("기본 정보", y)
    y += field("등록일",  "2026-07-01", y, "cal")
    y += field("진행단계","접수",        y, "drop", True)
    y += field("구분",    "기술지원",    y, "drop", True)
    y += 8
    y = section("현장 정보", y)
    y += field("현장명",  "",            y)
    y += field("담당자",  "",            y)
    y += 8
    y = section("일정",     y)
    y += field("요청일",  "",            y, "cal")
    y += field("예정일",  "",            y, "cal")

    # 저장 버튼
    by = min(y+20, H-160)
    rr(d, m, by, m+cw, by+110, r=55, fill=P)
    tc(d, "등록하기", m, by+32, cw, F(44,True), 'white')
    return img

def scr_signin(W, H):
    img = Image.new("RGB", (W,H), P_DK)
    gradient_rect(img, 0, 0, W, H//2+60, P_DK, P)
    gradient_rect(img, 0, H//2+60, W, H, P, BG)
    d = ImageDraw.Draw(img)
    statusbar(d, W, 0, 72, dark=True)

    # 아이콘 영역
    isz = int(W*0.30)
    ix, iy = (W-isz)//2, int(H*0.12)
    rr(d, ix, iy, ix+isz, iy+isz, r=int(isz*0.22), fill=(255,255,255,0))
    rr(d, ix, iy, ix+isz, iy+isz, r=int(isz*0.22), outline=(255,255,255,60), lw=4)
    tc(d, "DS", ix, iy+int(isz*0.08), isz, F(int(isz*0.30),True), 'white')
    tc(d, "HUB", ix, iy+int(isz*0.44), isz, F(int(isz*0.14)), ACC)
    gear(d, W//2, iy+int(isz*0.74), isz*0.19, isz*0.12, isz*0.07, 10, 'white', P)

    # 타이틀
    ty = iy+isz+52
    tc(d, "DS허브", 0, ty, W, F(int(W*0.09),True), 'white')
    tc(d, "현장 기술지원 현황을 한눈에", 0, ty+int(W*0.10)+8, W, F(int(W*0.042)), ACC)

    # 하단 카드
    cy = int(H*0.53)
    rr(d, 60, cy, W-60, H-60, r=36, fill=SURF)
    cy += 52
    tc(d, "시작하기", 60, cy, W-120, F(int(W*0.055),True), T1); cy += int(W*0.063)+14
    tc(d, "Google 계정으로 로그인하여", 60, cy, W-120, F(int(W*0.038)), T2); cy += int(W*0.046)
    tc(d, "Google Sheets와 실시간 연동하세요.", 60, cy, W-120, F(int(W*0.038)), T2); cy += int(W*0.055)+36
    bw = W-200
    rr(d, 100, cy, 100+bw, cy+120, r=60, fill=P)
    tc(d, "Google 계정으로 로그인", 100, cy+36, bw, F(int(W*0.040),True), 'white')
    return img

# ── 앱 아이콘 (512×512) ────────────────────────────────────────────────────
def make_icon(size):
    img = Image.new("RGBA", (size,size), (0,0,0,0))
    d = ImageDraw.Draw(img)
    r = int(size*0.22)
    d.rounded_rectangle([0,0,size-1,size-1], radius=r, fill=P_DK)
    # 상단 그라디언트 오버레이
    for i in range(size//2):
        t = i/(size//2)
        c = tuple(int(P_LT[j]*(1-t)+P_DK[j]*t) for j in range(3))
        alpha = int(80*(1-t))
        d.line([0,i,size,i], fill=(*c, alpha))
    # 장식 원 (오른쪽 하단)
    d.ellipse([size*0.55, size*0.55, size*1.1, size*1.1], fill=(*P, 60))
    # "DS" 텍스트
    fi = F(int(size*0.27), True)
    bb = d.textbbox((0,0), "DS", font=fi)
    d.text(((size-(bb[2]-bb[0]))//2, int(size*0.08)), "DS", font=fi, fill='white')
    # "HUB" 서브텍스트
    fi2 = F(int(size*0.09))
    bb2 = d.textbbox((0,0), "HUB", font=fi2)
    d.text(((size-(bb2[2]-bb2[0]))//2, int(size*0.37)), "HUB", font=fi2, fill=ACC)
    # 구분선
    lx = int(size*0.16)
    d.line([lx, int(size*0.50), size-lx, int(size*0.50)], fill=(*ACC, 180), width=3)
    # 기어
    gear(d, size//2, int(size*0.71), size*0.21, size*0.14, size*0.08, 10, 'white', P_DK)
    return img

# ── 피처 그래픽 (1024×500) ────────────────────────────────────────────────
def make_feature():
    W, H = 1024, 500
    img = Image.new("RGB", (W,H))
    gradient_rect(img, 0, 0, W, H, P_DK, (13,71,161))
    d = ImageDraw.Draw(img)
    # 장식 원
    d.ellipse([W-300,-80, W+80, H+80], fill=(*P_LT, 60))
    d.ellipse([W-240,-40, W+20, H+40], fill=(*P, 80))
    # 작은 원들
    for (cx2,cy2,r2) in [(820,80,40),(900,380,28),(760,420,18)]:
        d.ellipse([cx2-r2,cy2-r2,cx2+r2,cy2+r2], fill=(*ACC, 80))
    # 아이콘
    isz = 160
    icon_img = make_icon(isz).convert("RGBA")
    bg_patch = Image.new("RGBA", (isz,isz),(0,0,0,0))
    img_rgba = img.convert("RGBA")
    img_rgba.paste(icon_img, (72, (H-isz)//2), icon_img)
    img = img_rgba.convert("RGB")
    d = ImageDraw.Draw(img)
    # 텍스트
    tx, ty = 260, H//2-88
    d.text((tx, ty),    "DS허브",               font=F(90,True), fill='white')
    d.text((tx, ty+104),"현장 기술지원 현황을 한눈에", font=F(38),     fill=ACC)
    d.text((tx, ty+154),"Google Sheets 실시간 연동",   font=F(32),     fill=(173,216,255))
    # 태그
    tags = ["✓ 진행단계 관리", "✓ 현장 지도 연동", "✓ 실시간 동기화"]
    tx2 = tx
    for tag in tags:
        d.text((tx2, ty+210), tag, font=F(28,True), fill='white')
        bb = d.textbbox((0,0), tag, font=F(28,True))
        tx2 += bb[2]-bb[0]+36
    return img

# ── 생성 ──────────────────────────────────────────────────────────────────
print("🎨 에셋 생성 중...\n")

# 1. 앱 아이콘
icon512 = make_icon(512)
icon_rgb = Image.new("RGB", (512,512), P_DK)
icon_rgb.paste(icon512, mask=icon512.split()[3])
icon_rgb.save(f"{OUT}/icon_512.png", "PNG")

SIZES = {"mipmap-mdpi":48,"mipmap-hdpi":72,"mipmap-xhdpi":96,
         "mipmap-xxhdpi":144,"mipmap-xxxhdpi":192}
for folder, sz in SIZES.items():
    resized = icon512.resize((sz,sz), Image.LANCZOS)
    rgb = Image.new("RGB", (sz,sz), P_DK)
    rgb.paste(resized, mask=resized.split()[3])
    rgb.save(f"{RES}/{folder}/ic_launcher.png", "PNG")
    mask = Image.new("L", (sz,sz), 0)
    ImageDraw.Draw(mask).ellipse([0,0,sz-1,sz-1], fill=255)
    rd = Image.new("RGBA", (sz,sz), (0,0,0,0))
    rd.paste(resized, mask=mask)
    rgb2 = Image.new("RGB", (sz,sz), P_DK)
    rgb2.paste(rd, mask=rd.split()[3])
    rgb2.save(f"{RES}/{folder}/ic_launcher_round.png", "PNG")
print(f"  ✅ 앱 아이콘 512px + 5가지 밀도")

# 2. 피처 그래픽
make_feature().save(f"{OUT}/feature_graphic_1024x500.png", "PNG")
print(f"  ✅ 피처 그래픽 1024×500")

# 3. 스크린샷 생성
configs = [
    ("phone",   1080, 1920),
    ("tablet7", 1200, 1920),
    ("tablet10",1600, 2560),
]

screens_fn = [
    ("01_홈_목록",    lambda W,H: scr_home(W, H, "전체")),
    ("02_상세보기",   lambda W,H: scr_detail(W, H)),
    ("03_등록화면",   lambda W,H: scr_form(W, H)),
    ("04_필터_처리중",lambda W,H: scr_home(W, H, "처리중")),
]

for device, W, H in configs:
    for fname, fn in screens_fn:
        img = fn(W, H)
        img.save(f"{OUT}/{device}/{device}_{fname}.png", "PNG")
    print(f"  ✅ {device} 스크린샷 {len(screens_fn)}장 ({W}×{H})")

print(f"\n📁 저장 위치: {OUT}/")
print("\n" + "="*55)
print("📝 Play Store 등록정보 (복사해서 붙여넣기)")
print("="*55)
print(f"\n앱 이름 (최대 30자):\nDS허브 – 기술지원 현장관리\n")
print(f"간단한 설명 (최대 80자):\n현장 기술지원을 스마트하게. Google Sheets와 실시간 연동하여 업무 현황을 어디서나 관리하세요.\n")
print("""자세한 설명 (최대 4000자):
DS허브는 DS 기술지원 팀을 위한 현장 업무 관리 앱입니다.
별도의 서버나 데이터베이스 없이 기존에 사용하던 Google Sheets와 직접 연동하여, 팀원 모두가 실시간으로 기술지원 현황을 확인하고 관리할 수 있습니다.

■ 주요 기능

• 기술지원 현황 관리
  - 접수 / 담당지정 / 처리중 / 완료 / 보류 / 취소 단계별 진행 관리
  - 장애처리, 기술지원, 이전설치, 신규설치, 점검, 철거, 신규개발 구분 관리

• 현장 정보 관리
  - 현장명으로 지도 앱과 바로 연계하여 위치 확인
  - 담당자 배정 및 이력 관리

• 일정 추적
  - 등록일 / 요청일 / 예정일 / 완료일 날짜별 관리

• 빠른 검색 & 필터
  - 현장명, 담당자, 요청사항 통합 검색
  - 진행단계별 필터로 빠른 현황 파악

• Google Sheets 직접 연동
  - 기존 Google Sheets 스프레드시트와 즉시 연결
  - 앱에서 입력한 내용이 시트에 실시간 반영
  - 시트에서 수정한 내용도 앱에서 바로 확인

■ 이런 분께 추천합니다
✓ IT 현장 기술지원 업무를 담당하는 팀
✓ Google Sheets로 업무를 관리하고 있는 조직
✓ 현장에서 모바일로 빠르게 업무 현황을 확인해야 하는 담당자

■ 사용 방법
1. Google 계정으로 로그인
2. 설정에서 연동할 Google Sheets 스프레드시트 ID 입력
3. 기술지원 내역 등록, 수정, 조회 시작

* Google Sheets 읽기/쓰기 권한이 필요합니다.
* 인터넷 연결이 필요합니다.""")
EOF