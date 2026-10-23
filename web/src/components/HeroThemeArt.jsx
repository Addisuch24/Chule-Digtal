/**
 * Page-Specific Themed SVG Background Illustrations
 * High-contrast, beautifully defined vector illustrations inspired by the design references.
 * Features 3D-inspired elements, rich multi-tone gradients, bold outlines, and the brand palette:
 * Primary Navy (#17324D), Secondary Blue (#355C7D), Emerald (#15966A), Accent Teal (#0F8B8D),
 * Gold (#D99A26), Success (#16B65B), Error/Alert (#DC4545), and Info (#2878C7).
 */

export function FinanceHeroArt() {
  return (
    <svg
      viewBox="0 0 400 150"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="finTrendGrad" x1="40" y1="125" x2="360" y2="20" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#15966A" />
          <stop offset="60%" stopColor="#16B65B" />
          <stop offset="100%" stopColor="#10B981" />
        </linearGradient>
        <linearGradient id="coinGradGold" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#FEF3C7" />
          <stop offset="50%" stopColor="#F59E0B" />
          <stop offset="85%" stopColor="#D99A26" />
          <stop offset="100%" stopColor="#B45309" />
        </linearGradient>
        <linearGradient id="coinGradGold2" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#FEF3C7" />
          <stop offset="100%" stopColor="#D99A26" />
        </linearGradient>
        <filter id="finDropShadow" x="-10%" y="-10%" width="125%" height="125%">
          <feDropShadow dx="2" dy="4" stdDeviation="3.5" floodColor="#0E3E2E" floodOpacity="0.18" />
        </filter>
      </defs>

      {/* Ascending Trend Sparkline with Area Fill */}
      <path
        d="M 30 130 C 85 125, 140 95, 210 75 T 325 36 L 365 18"
        stroke="url(#finTrendGrad)"
        strokeWidth="4"
        strokeLinecap="round"
        fill="none"
      />
      <path
        d="M 30 130 C 85 125, 140 95, 210 75 T 325 36 L 365 18 L 365 142 L 30 142 Z"
        fill="url(#finTrendGrad)"
        fillOpacity="0.14"
      />

      {/* Growth Trend Arrowhead */}
      <polygon points="376,12 360,14 367,25" fill="#16B65B" stroke="#0E3E2E" strokeWidth="1.2" />

      {/* Trendline Node Circles */}
      <circle cx="365" cy="18" r="6" fill="#16B65B" stroke="#FFFFFF" strokeWidth="2.5" />
      <circle cx="365" cy="18" r="13" stroke="#16B65B" strokeWidth="2" strokeDasharray="4 3" opacity="0.75" />
      <circle cx="210" cy="75" r="5" fill="#0F8B8D" stroke="#FFFFFF" strokeWidth="2" />
      <circle cx="120" cy="104" r="5" fill="#15966A" stroke="#FFFFFF" strokeWidth="2" />

      {/* Stylized Banknote / Treasury Bill in Background */}
      <g transform="translate(75, 16)" filter="url(#finDropShadow)">
        <rect
          x="0"
          y="0"
          width="122"
          height="70"
          rx="8"
          stroke="#0E3E2E"
          strokeWidth="2.6"
          fill="#F0FDF4"
          transform="rotate(-9 61 35)"
        />
        {/* Guilloche Border Inset */}
        <rect
          x="6"
          y="6"
          width="110"
          height="58"
          rx="5"
          stroke="#15966A"
          strokeWidth="1.6"
          strokeDasharray="4 2"
          fill="none"
          transform="rotate(-9 61 35)"
        />
        {/* Central Seal */}
        <circle cx="61" cy="35" r="17" stroke="#D99A26" strokeWidth="2.4" fill="#FEF3C7" fillOpacity="0.8" transform="rotate(-9 61 35)" />
        <text x="61" y="39" textAnchor="middle" fill="#0E3E2E" fontSize="11" fontWeight="900" transform="rotate(-9 61 35)">
          ETB
        </text>
        {/* Decorative corner lines */}
        <line x1="14" y1="14" x2="34" y2="14" stroke="#15966A" strokeWidth="2.6" strokeLinecap="round" transform="rotate(-9 61 35)" />
        <line x1="88" y1="56" x2="108" y2="56" stroke="#15966A" strokeWidth="2.6" strokeLinecap="round" transform="rotate(-9 61 35)" />
      </g>

      {/* Primary Stack of 3D Golden Coins */}
      <g transform="translate(198, 38)" filter="url(#finDropShadow)">
        {/* Coin 1 (Bottom) */}
        <g>
          <path d="M 4 58 C 4 68, 80 68, 80 58 L 80 70 C 80 80, 4 80, 4 70 Z" fill="#B45309" stroke="#0E3E2E" strokeWidth="2.4" />
          <ellipse cx="42" cy="58" rx="38" ry="13" fill="url(#coinGradGold)" stroke="#0E3E2E" strokeWidth="2.6" />
          <ellipse cx="42" cy="58" rx="30" ry="9" stroke="#0E3E2E" strokeWidth="1.2" strokeDasharray="4 2" />
        </g>
        {/* Coin 2 (Middle) */}
        <g>
          <path d="M 4 40 C 4 50, 80 50, 80 40 L 80 52 C 80 62, 4 62, 4 52 Z" fill="#D99A26" stroke="#0E3E2E" strokeWidth="2.4" />
          <ellipse cx="42" cy="40" rx="38" ry="13" fill="url(#coinGradGold)" stroke="#0E3E2E" strokeWidth="2.6" />
          <ellipse cx="42" cy="40" rx="30" ry="9" stroke="#0E3E2E" strokeWidth="1.2" strokeDasharray="4 2" />
        </g>
        {/* Coin 3 (Top) */}
        <g>
          <path d="M 4 22 C 4 32, 80 32, 80 22 L 80 34 C 80 44, 4 44, 4 34 Z" fill="#D99A26" stroke="#0E3E2E" strokeWidth="2.4" />
          <ellipse cx="42" cy="22" rx="38" ry="13" fill="url(#coinGradGold)" stroke="#0E3E2E" strokeWidth="2.8" />
          <ellipse cx="42" cy="22" rx="30" ry="9" stroke="#0E3E2E" strokeWidth="1.6" />
          {/* Bold Currency Emblem "ብር" */}
          <text x="42" y="27" textAnchor="middle" fill="#0E3E2E" fontSize="15" fontWeight="900" fontFamily="sans-serif">
            ብር
          </text>
        </g>
      </g>

      {/* Secondary Coin Stack on Right */}
      <g transform="translate(290, 62)" filter="url(#finDropShadow)">
        <path d="M 4 22 C 4 30, 56 30, 56 22 L 56 32 C 56 40, 4 40, 4 32 Z" fill="#B45309" stroke="#0E3E2E" strokeWidth="2.2" />
        <ellipse cx="30" cy="22" rx="26" ry="9" fill="url(#coinGradGold2)" stroke="#0E3E2E" strokeWidth="2.4" />
        <path d="M 4 10 C 4 18, 56 18, 56 10 L 56 20 C 56 28, 4 28, 4 20 Z" fill="#D99A26" stroke="#0E3E2E" strokeWidth="2.2" />
        <ellipse cx="30" cy="10" rx="26" ry="9" fill="url(#coinGradGold2)" stroke="#0E3E2E" strokeWidth="2.4" />
      </g>
    </svg>
  );
}

export function CustomersHeroArt() {
  return (
    <svg
      viewBox="0 0 400 150"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="custGradSuit" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#1E5088" />
          <stop offset="100%" stopColor="#17324D" />
        </linearGradient>
        <linearGradient id="custGradSuitLeft" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#355C7D" />
          <stop offset="100%" stopColor="#17324D" />
        </linearGradient>
        <linearGradient id="custGradSuitRight" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#0F8B8D" />
          <stop offset="100%" stopColor="#15966A" />
        </linearGradient>
        <filter id="custShadow" x="-10%" y="-10%" width="125%" height="125%">
          <feDropShadow dx="2" dy="4" stdDeviation="3.5" floodColor="#0F3762" floodOpacity="0.18" />
        </filter>
      </defs>

      {/* Network Connection Line */}
      <path
        d="M 60 74 C 110 52, 160 88, 220 54 T 350 48"
        stroke="#2878C7"
        strokeWidth="2.6"
        strokeDasharray="6 4"
        opacity="0.8"
      />

      {/* Floating Circular Avatar Badge Chips (as in reference image) */}
      <g transform="translate(85, 26)" filter="url(#custShadow)">
        <circle cx="16" cy="16" r="16" fill="#2878C7" stroke="#FFFFFF" strokeWidth="2.5" />
        <circle cx="16" cy="12" r="5.5" fill="#FFFFFF" />
        <path d="M 8 26 C 8 20, 11 18, 16 18 C 21 18, 24 20, 24 26" fill="#FFFFFF" />
      </g>
      <g transform="translate(338, 22)" filter="url(#custShadow)">
        <circle cx="16" cy="16" r="16" fill="#0F8B8D" stroke="#FFFFFF" strokeWidth="2.5" />
        <circle cx="16" cy="12" r="5.5" fill="#FFFFFF" />
        <path d="M 8 26 C 8 20, 11 18, 16 18 C 21 18, 24 20, 24 26" fill="#FFFFFF" />
      </g>

      {/* Left Team Member Avatar */}
      <g transform="translate(162, 34)" filter="url(#custShadow)">
        <circle cx="28" cy="22" r="17" stroke="#0F3762" strokeWidth="2.6" fill="#E0F2FE" />
        <path
          d="M 6 72 C 6 48, 17 42, 28 42 C 39 42, 50 48, 50 72"
          stroke="#0F3762"
          strokeWidth="2.6"
          strokeLinecap="round"
          fill="url(#custGradSuitLeft)"
        />
        {/* Shirt collar notch */}
        <polygon points="28,42 32,54 28,60 24,54" fill="#FFFFFF" />
      </g>

      {/* Right Team Member Avatar */}
      <g transform="translate(282, 34)" filter="url(#custShadow)">
        <circle cx="28" cy="22" r="17" stroke="#0F3762" strokeWidth="2.6" fill="#F0FDF4" />
        <path
          d="M 6 72 C 6 48, 17 42, 28 42 C 39 42, 50 48, 50 72"
          stroke="#0F3762"
          strokeWidth="2.6"
          strokeLinecap="round"
          fill="url(#custGradSuitRight)"
        />
        {/* Shirt collar notch */}
        <polygon points="28,42 32,54 28,60 24,54" fill="#FFFFFF" />
      </g>

      {/* Central Prominent Leader Avatar (as in reference image) */}
      <g transform="translate(216, 16)" filter="url(#custShadow)">
        {/* Head */}
        <circle cx="38" cy="28" r="22" stroke="#0F3762" strokeWidth="3" fill="#F0FDFA" />
        {/* Torso / Suit */}
        <path
          d="M 6 86 C 6 56, 20 48, 38 48 C 56 48, 70 56, 70 86"
          stroke="#0F3762"
          strokeWidth="3"
          strokeLinecap="round"
          fill="url(#custGradSuit)"
        />
        {/* White shirt collar */}
        <polygon points="38,48 44,64 38,72 32,64" fill="#FFFFFF" stroke="#0F3762" strokeWidth="1.5" />
        {/* Emerald green tie */}
        <polygon points="38,58 42,76 38,84 34,76" fill="#15966A" stroke="#0F3762" strokeWidth="1" />

        {/* Verified User Badge */}
        <circle cx="60" cy="24" r="10.5" fill="#16B65B" stroke="#FFFFFF" strokeWidth="2.4" />
        <path d="M 56 24 L 59 27 L 65 21" stroke="#FFFFFF" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
      </g>
    </svg>
  );
}

export function OrdersHeroArt() {
  return (
    <svg
      viewBox="0 0 400 150"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="orderGradPath" x1="0" y1="0" x2="400" y2="150" gradientUnits="userSpaceOnUse">
          <stop offset="0%" stopColor="#2878C7" />
          <stop offset="100%" stopColor="#7C3AED" />
        </linearGradient>
        <linearGradient id="boxGradKraft" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#FEF3C7" />
          <stop offset="100%" stopColor="#FDE68A" />
        </linearGradient>
        <filter id="orderDropShadow" x="-10%" y="-10%" width="125%" height="125%">
          <feDropShadow dx="2" dy="4" stdDeviation="4" floodColor="#1A284F" floodOpacity="0.18" />
        </filter>
      </defs>

      {/* Swift Order Flow Line */}
      <path
        d="M 40 115 C 100 115, 115 52, 210 52"
        stroke="url(#orderGradPath)"
        strokeWidth="3.2"
        strokeLinecap="round"
        strokeDasharray="6 4"
      />
      <circle cx="40" cy="115" r="5" fill="#2878C7" stroke="#FFFFFF" strokeWidth="2" />

      {/* Metal Wire Shopping Cart (directly inspired by reference images) */}
      <g transform="translate(100, 24)" filter="url(#orderDropShadow)">
        {/* Handle */}
        <path d="M 12 36 L 24 36 L 36 68" stroke="#2878C7" strokeWidth="3.2" strokeLinecap="round" strokeLinejoin="round" />
        <rect x="8" y="32" width="12" height="8" rx="3" fill="#17324D" />

        {/* Basket Wireframe */}
        <polygon points="24,40 82,36 74,78 30,78" stroke="#1A284F" strokeWidth="2.8" fill="#EFF6FF" fillOpacity="0.8" />
        {/* Grid wire lines */}
        <line x1="26" y1="52" x2="79" y2="49" stroke="#2878C7" strokeWidth="1.8" />
        <line x1="28" y1="65" x2="76" y2="63" stroke="#2878C7" strokeWidth="1.8" />
        <line x1="42" y1="39" x2="43" y2="78" stroke="#2878C7" strokeWidth="1.8" />
        <line x1="56" y1="38" x2="57" y2="78" stroke="#2878C7" strokeWidth="1.8" />
        <line x1="70" y1="37" x2="69" y2="78" stroke="#2878C7" strokeWidth="1.8" />

        {/* Chassis & Wheels */}
        <path d="M 32 78 L 36 90 L 72 90" stroke="#1A284F" strokeWidth="2.8" strokeLinecap="round" />
        <circle cx="36" cy="94" r="7" fill="#17324D" stroke="#1A284F" strokeWidth="2" />
        <circle cx="36" cy="94" r="2.8" fill="#FFFFFF" />
        <circle cx="70" cy="94" r="7" fill="#17324D" stroke="#1A284F" strokeWidth="2" />
        <circle cx="70" cy="94" r="2.8" fill="#FFFFFF" />
      </g>

      {/* Prominent Stacked 3D Delivery Parcel Boxes (as in reference image) */}
      <g transform="translate(225, 20)" filter="url(#orderDropShadow)">
        {/* Primary Box - Top Face */}
        <polygon
          points="52,6 96,27 52,48 8,27"
          stroke="#1A284F"
          strokeWidth="2.8"
          fill="#FEF3C7"
        />
        {/* Left Face */}
        <polygon
          points="8,27 52,48 52,98 8,77"
          stroke="#1A284F"
          strokeWidth="2.8"
          fill="#FDE68A"
        />
        {/* Right Face */}
        <polygon
          points="52,48 96,27 96,77 52,98"
          stroke="#1A284F"
          strokeWidth="2.8"
          fill="#FCD34D"
        />

        {/* Purple Packaging Tape (signature accent from login/reference) */}
        <line x1="30" y1="16.5" x2="74" y2="37.5" stroke="#7C3AED" strokeWidth="4.8" strokeOpacity="0.95" />
        <line x1="52" y1="48" x2="52" y2="98" stroke="#7C3AED" strokeWidth="3.6" strokeOpacity="0.95" />

        {/* Shipping Label with Barcode */}
        <rect x="62" y="55" width="26" height="18" rx="2.5" fill="#FFFFFF" stroke="#1A284F" strokeWidth="1.4" />
        <line x1="66" y1="59" x2="66" y2="68" stroke="#1A284F" strokeWidth="1.6" />
        <line x1="71" y1="59" x2="71" y2="68" stroke="#1A284F" strokeWidth="1.6" />
        <line x1="76" y1="59" x2="76" y2="68" stroke="#1A284F" strokeWidth="2.4" />
        <line x1="82" y1="59" x2="82" y2="68" stroke="#1A284F" strokeWidth="1.4" />
      </g>

      {/* Secondary Parcel Box Stacked Beside */}
      <g transform="translate(310, 48)" filter="url(#orderDropShadow)">
        <polygon points="34,4 64,18 34,32 4,18" stroke="#1A284F" strokeWidth="2.4" fill="#EDE9FE" />
        <polygon points="4,18 34,32 34,68 4,54" stroke="#1A284F" strokeWidth="2.4" fill="#DDD6FE" />
        <polygon points="34,32 64,18 64,54 34,68" stroke="#1A284F" strokeWidth="2.4" fill="#C4B5FD" />
        <line x1="19" y1="11" x2="49" y2="25" stroke="#7C3AED" strokeWidth="3.6" />
      </g>

      {/* Order Clipboard / Checklist */}
      <g transform="translate(195, 54)" filter="url(#orderDropShadow)">
        <rect x="0" y="0" width="36" height="50" rx="4" stroke="#1A284F" strokeWidth="2.2" fill="#FFFFFF" />
        <rect x="11" y="-4" width="14" height="7" rx="2" fill="#2878C7" stroke="#1A284F" strokeWidth="1.4" />
        <path d="M 7 15 L 11 19 L 20 11" stroke="#15966A" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
        <line x1="7" y1="28" x2="28" y2="28" stroke="#1A284F" strokeWidth="1.8" strokeLinecap="round" />
        <line x1="7" y1="38" x2="22" y2="38" stroke="#64748B" strokeWidth="1.8" strokeLinecap="round" />
      </g>
    </svg>
  );
}

export function NotesHeroArt() {
  return (
    <svg
      viewBox="0 0 400 150"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <defs>
        <filter id="noteDropShadow" x="-10%" y="-10%" width="125%" height="125%">
          <feDropShadow dx="2" dy="4" stdDeviation="4" floodColor="#38240D" floodOpacity="0.16" />
        </filter>
      </defs>

      {/* Ambient Grid Dots */}
      <g opacity="0.6" fill="#D99A26">
        <circle cx="75" cy="30" r="2.2" />
        <circle cx="100" cy="30" r="2.2" />
        <circle cx="125" cy="30" r="2.2" />
        <circle cx="75" cy="54" r="2.2" />
        <circle cx="100" cy="54" r="2.2" />
        <circle cx="125" cy="54" r="2.2" />
      </g>

      {/* Pinned Memo Card with Folded Corner */}
      <g transform="translate(120, 22)" filter="url(#noteDropShadow)">
        <polygon
          points="0,0 66,0 82,16 82,78 0,78"
          stroke="#38240D"
          strokeWidth="2.6"
          fill="#FFFDF8"
          transform="rotate(-5 41 39)"
        />
        {/* Folded Flap */}
        <polygon
          points="66,0 66,16 82,16"
          stroke="#38240D"
          strokeWidth="2.2"
          fill="#D99A26"
          fillOpacity="0.4"
          transform="rotate(-5 41 39)"
        />
        {/* Pushpin */}
        <circle cx="40" cy="8" r="5" fill="#D99A26" stroke="#38240D" strokeWidth="1.6" transform="rotate(-5 41 39)" />
        {/* Ruled Note Lines */}
        <line x1="14" y1="26" x2="62" y2="26" stroke="#D99A26" strokeWidth="2.4" strokeLinecap="round" transform="rotate(-5 41 39)" />
        <line x1="14" y1="42" x2="56" y2="42" stroke="#64748B" strokeWidth="2" strokeLinecap="round" transform="rotate(-5 41 39)" />
        <line x1="14" y1="56" x2="46" y2="56" stroke="#64748B" strokeWidth="2" strokeLinecap="round" transform="rotate(-5 41 39)" />
      </g>

      {/* Prominent Open Spiral Notebook (directly inspired by reference images) */}
      <g transform="translate(208, 16)" filter="url(#noteDropShadow)">
        {/* Book Spine / Cover Shadow */}
        <rect x="4" y="6" width="110" height="92" rx="9" fill="#D99A26" fillOpacity="0.28" />

        {/* Left Page */}
        <path
          d="M 8 10 C 8 5, 12 3, 16 3 L 60 3 L 60 94 L 16 94 C 12 94, 8 92, 8 87 Z"
          stroke="#38240D"
          strokeWidth="3"
          fill="#FFFDF9"
        />
        {/* Left Page Writing lines & check marks */}
        <path d="M 18 24 L 22 28 L 29 20" stroke="#15966A" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
        <line x1="36" y1="24" x2="54" y2="24" stroke="#38240D" strokeWidth="2" strokeLinecap="round" />
        <path d="M 18 42 L 22 46 L 29 38" stroke="#15966A" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round" />
        <line x1="36" y1="42" x2="54" y2="42" stroke="#64748B" strokeWidth="2" strokeLinecap="round" />
        <line x1="18" y1="62" x2="50" y2="62" stroke="#64748B" strokeWidth="2" strokeLinecap="round" />
        <line x1="18" y1="78" x2="42" y2="78" stroke="#64748B" strokeWidth="2" strokeLinecap="round" />

        {/* Right Page */}
        <path
          d="M 60 3 L 104 3 C 108 3, 112 5, 112 10 L 112 87 C 112 92, 108 94, 104 94 L 60 94 Z"
          stroke="#38240D"
          strokeWidth="3"
          fill="#FFFFFF"
        />
        {/* Ruled lines on right page */}
        <line x1="70" y1="20" x2="102" y2="20" stroke="#D99A26" strokeWidth="2.4" strokeLinecap="round" />
        <line x1="70" y1="36" x2="102" y2="36" stroke="#64748B" strokeWidth="2" strokeLinecap="round" />
        <line x1="70" y1="52" x2="102" y2="52" stroke="#64748B" strokeWidth="2" strokeLinecap="round" />
        <line x1="70" y1="68" x2="94" y2="68" stroke="#64748B" strokeWidth="2" strokeLinecap="round" />
        <line x1="70" y1="82" x2="98" y2="82" stroke="#64748B" strokeWidth="2" strokeLinecap="round" />

        {/* Spiral Binder Metallic Rings */}
        {[14, 28, 42, 56, 70, 84].map((y) => (
          <g key={y}>
            <ellipse cx="60" cy={y} rx="7" ry="4.5" fill="#D99A26" stroke="#38240D" strokeWidth="1.6" />
            <ellipse cx="60" cy={y - 1} rx="4.5" ry="2.2" fill="#FFFFFF" fillOpacity="0.8" />
          </g>
        ))}

        {/* Bookmark Ribbon */}
        <path d="M 92 94 L 92 110 L 98 103 L 104 110 L 104 94 Z" fill="#15966A" stroke="#38240D" strokeWidth="1.6" />
      </g>

      {/* Prominent Drafting Fountain Pen */}
      <g transform="translate(312, 14) rotate(32)" filter="url(#noteDropShadow)">
        {/* Pen Barrel */}
        <rect x="0" y="0" width="11" height="78" rx="3.5" fill="#17324D" stroke="#38240D" strokeWidth="2.2" />
        {/* Gold Accent Ring */}
        <rect x="0" y="26" width="11" height="6" fill="#D99A26" stroke="#38240D" strokeWidth="1.2" />
        <rect x="0" y="70" width="11" height="3.5" fill="#D99A26" />
        {/* Pen Clip */}
        <rect x="11" y="8" width="3.5" height="38" rx="1.8" fill="#D99A26" stroke="#38240D" strokeWidth="1.2" />
        {/* Gold Pen Nib */}
        <polygon points="0,78 11,78 5.5,96" fill="#D99A26" stroke="#38240D" strokeWidth="2" />
        <line x1="5.5" y1="79" x2="5.5" y2="89" stroke="#38240D" strokeWidth="1.4" />
        <circle cx="5.5" cy="85" r="1.4" fill="#38240D" />
      </g>
    </svg>
  );
}

export function DashboardHeroArt() {
  return (
    <svg
      viewBox="0 0 400 150"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="dashBar1Grad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#2878C7" />
          <stop offset="100%" stopColor="#17324D" />
        </linearGradient>
        <linearGradient id="dashBar2Grad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#0F8B8D" />
          <stop offset="100%" stopColor="#2878C7" />
        </linearGradient>
        <linearGradient id="dashBar3Grad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#15966A" />
          <stop offset="100%" stopColor="#0F8B8D" />
        </linearGradient>
        <linearGradient id="dashBar4Grad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#16B65B" />
          <stop offset="100%" stopColor="#15966A" />
        </linearGradient>
        <linearGradient id="dashArrowGrad" x1="0" y1="1" x2="1" y2="0">
          <stop offset="0%" stopColor="#15966A" />
          <stop offset="100%" stopColor="#16B65B" />
        </linearGradient>
        <filter id="dashDropShadow" x="-10%" y="-10%" width="125%" height="125%">
          <feDropShadow dx="2" dy="4" stdDeviation="3.5" floodColor="#11283D" floodOpacity="0.18" />
        </filter>
      </defs>

      {/* Telemetry Radial Gauge Arc with Scale Ticks */}
      <path
        d="M 85 124 A 76 76 0 0 1 205 48"
        stroke="#0F8B8D"
        strokeWidth="3.6"
        strokeLinecap="round"
        fill="none"
      />
      <path
        d="M 110 118 A 60 60 0 0 1 195 64"
        stroke="#2878C7"
        strokeWidth="2"
        strokeDasharray="4 4"
        fill="none"
        opacity="0.8"
      />
      {/* Gauge Scale Dots */}
      <circle cx="85" cy="124" r="4.5" fill="#17324D" />
      <circle cx="130" cy="74" r="3.8" fill="#0F8B8D" />
      <circle cx="170" cy="54" r="3.8" fill="#0F8B8D" />
      <circle cx="205" cy="48" r="5.5" fill="#15966A" stroke="#FFFFFF" strokeWidth="1.8" />

      {/* Ascending 3D Metric Bar Pillars (matching reference images) */}
      <g transform="translate(175, 24)" filter="url(#dashDropShadow)">
        {/* Baseline Axis */}
        <line x1="-12" y1="104" x2="114" y2="104" stroke="#17324D" strokeWidth="2.5" strokeLinecap="round" opacity="0.5" />

        {/* Bar 1 */}
        <rect x="0" y="62" width="18" height="42" rx="4" fill="url(#dashBar1Grad)" stroke="#17324D" strokeWidth="2" />
        {/* Bar 2 */}
        <rect x="26" y="44" width="18" height="60" rx="4" fill="url(#dashBar2Grad)" stroke="#17324D" strokeWidth="2" />
        {/* Bar 3 */}
        <rect x="52" y="26" width="18" height="78" rx="4" fill="url(#dashBar3Grad)" stroke="#0E3E2E" strokeWidth="2" />
        {/* Bar 4 (Peak) */}
        <rect x="78" y="8" width="18" height="96" rx="4" fill="url(#dashBar4Grad)" stroke="#0E3E2E" strokeWidth="2.4" />

        {/* Dynamic Soaring Green Growth Trend Arrow */}
        <path
          d="M 8 54 Q 35 38, 60 20 T 94 0"
          stroke="url(#dashArrowGrad)"
          strokeWidth="4"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
        {/* Arrowhead */}
        <polygon points="103,-6 91,-2 97,8" fill="#16B65B" stroke="#0E3E2E" strokeWidth="1.2" />

        {/* Glowing Data Point Nodes */}
        <circle cx="8" cy="54" r="5" fill="#FFFFFF" stroke="#2878C7" strokeWidth="2.8" />
        <circle cx="35" cy="38" r="5" fill="#FFFFFF" stroke="#0F8B8D" strokeWidth="2.8" />
        <circle cx="61" cy="20" r="5" fill="#FFFFFF" stroke="#15966A" strokeWidth="2.8" />
        <circle cx="87" cy="4" r="6" fill="#FFFFFF" stroke="#16B65B" strokeWidth="3.2" />
      </g>

      {/* Analytics Document Card in Front of Bars (as in reference image) */}
      <g transform="translate(138, 72)" filter="url(#dashDropShadow)">
        <rect x="0" y="0" width="46" height="36" rx="4" fill="#FFFFFF" stroke="#17324D" strokeWidth="1.8" />
        {/* Mini chart lines */}
        <line x1="6" y1="10" x2="26" y2="10" stroke="#17324D" strokeWidth="1.6" strokeLinecap="round" />
        <path d="M 6 26 L 14 20 L 22 23 L 34 14 L 40 18" stroke="#15966A" strokeWidth="1.8" strokeLinecap="round" fill="none" />
      </g>

      {/* Concentric Business Compass / Telemetry Dial */}
      <g transform="translate(320, 24)">
        <circle cx="36" cy="36" r="35" stroke="#17324D" strokeWidth="2.8" strokeDasharray="6 4" opacity="0.7" />
        <circle cx="36" cy="36" r="23" stroke="#0F8B8D" strokeWidth="2.4" opacity="0.9" />
        <circle cx="36" cy="36" r="13" fill="#15966A" fillOpacity="0.3" stroke="#15966A" strokeWidth="2.2" />
        <circle cx="36" cy="36" r="6" fill="#16B65B" stroke="#FFFFFF" strokeWidth="1.8" />
        {/* Crosshair Axes */}
        <line x1="36" y1="-3" x2="36" y2="75" stroke="#17324D" strokeWidth="2.2" strokeDasharray="4 3" opacity="0.6" />
        <line x1="-3" y1="36" x2="75" y2="36" stroke="#17324D" strokeWidth="2.2" strokeDasharray="4 3" opacity="0.6" />
      </g>
    </svg>
  );
}

export function NotificationsHeroArt() {
  return (
    <svg
      viewBox="0 0 400 150"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <defs>
        <linearGradient id="notifGradBell" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#FFFFFF" />
          <stop offset="100%" stopColor="#EEF2FF" />
        </linearGradient>
        <linearGradient id="bellGoldGrad" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor="#FEF3C7" />
          <stop offset="50%" stopColor="#F59E0B" />
          <stop offset="100%" stopColor="#D99A26" />
        </linearGradient>
        <filter id="notifDropShadow" x="-10%" y="-10%" width="125%" height="125%">
          <feDropShadow dx="2" dy="4" stdDeviation="3.5" floodColor="#1E1B4B" floodOpacity="0.18" />
        </filter>
      </defs>

      {/* Broadcast Pulse Wave Rings */}
      <path
        d="M 110 74 A 58 58 0 0 1 185 24"
        stroke="#4F46E5"
        strokeWidth="2.8"
        strokeLinecap="round"
        strokeDasharray="6 4"
      />
      <path
        d="M 90 74 A 78 78 0 0 1 185 8"
        stroke="#4F46E5"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeOpacity="0.65"
      />

      {/* Prominent Notification Chime Bell (as in reference image) */}
      <g transform="translate(215, 14)" filter="url(#notifDropShadow)">
        {/* Acoustic Sound Waves Left */}
        <path d="M 4 38 C -5 48, -5 62, 4 72" stroke="#4F46E5" strokeWidth="3.4" strokeLinecap="round" />
        <path d="M -6 28 C -18 44, -18 76, -6 92" stroke="#4F46E5" strokeWidth="2.4" strokeLinecap="round" strokeOpacity="0.65" />

        {/* Bell Body */}
        <path
          d="M 44 14 C 27 14, 18 30, 18 56 L 14 74 C 12 80, 16 84, 23 84 L 65 84 C 72 84, 76 80, 74 74 L 70 56 C 70 30, 61 14, 44 14 Z"
          stroke="#1E1B4B"
          strokeWidth="3.2"
          fill="url(#notifGradBell)"
        />
        {/* Bell Clapper */}
        <circle cx="44" cy="92" r="8" fill="url(#bellGoldGrad)" stroke="#1E1B4B" strokeWidth="2.6" />
        {/* Bell Top Suspension Loop */}
        <path d="M 38 14 C 38 6, 50 6, 50 14" stroke="#1E1B4B" strokeWidth="3.2" fill="none" />

        {/* Acoustic Sound Waves Right */}
        <path d="M 84 38 C 93 48, 93 62, 84 72" stroke="#F59E0B" strokeWidth="3.4" strokeLinecap="round" />
        <path d="M 94 28 C 106 44, 106 76, 94 92" stroke="#F59E0B" strokeWidth="2.4" strokeLinecap="round" strokeOpacity="0.65" />

        {/* Vibrant Red Notification Badge Bubble (as in reference image) */}
        <circle cx="68" cy="22" r="12" fill="#DC4545" stroke="#FFFFFF" strokeWidth="2.5" />
        <text x="68" y="26.5" textAnchor="middle" fill="#FFFFFF" fontSize="12" fontWeight="900" fontFamily="sans-serif">
          3
        </text>
      </g>

      {/* Floating Chat / Alert Bubble */}
      <g transform="translate(130, 42)" filter="url(#notifDropShadow)">
        <rect x="0" y="0" width="54" height="40" rx="11" stroke="#1E1B4B" strokeWidth="2.6" fill="#FFFFFF" />
        <polygon points="12,40 25,40 14,51" fill="#FFFFFF" stroke="#1E1B4B" strokeWidth="2.2" strokeLinejoin="round" />
        <circle cx="16" cy="20" r="4" fill="#4F46E5" />
        <circle cx="27" cy="20" r="4" fill="#4F46E5" />
        <circle cx="38" cy="20" r="4" fill="#4F46E5" />
      </g>
    </svg>
  );
}

export function ProfileHeroArt() {
  return (
    <svg
      viewBox="0 0 400 150"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <defs>
        <filter id="profDropShadow" x="-10%" y="-10%" width="125%" height="125%">
          <feDropShadow dx="2" dy="4" stdDeviation="3.5" floodColor="#0E3429" floodOpacity="0.18" />
        </filter>
      </defs>

      {/* Biometric Halo Rings with Calibration Ticks */}
      <circle cx="275" cy="74" r="64" stroke="#0E3429" strokeWidth="2.2" strokeDasharray="6 4" opacity="0.5" />
      <circle cx="275" cy="74" r="50" stroke="#15966A" strokeWidth="2" opacity="0.7" />

      {/* Circular User Avatar Silhouette Medallion (as in reference image) */}
      <g transform="translate(235, 34)" filter="url(#profDropShadow)">
        <circle cx="40" cy="40" r="38" stroke="#0E3429" strokeWidth="3.2" fill="#F0FDF4" />
        {/* Silhouette Head */}
        <circle cx="40" cy="28" r="14" fill="#15966A" stroke="#0E3429" strokeWidth="2" />
        {/* Silhouette Shoulders */}
        <path
          d="M 16 66 C 16 48, 26 44, 40 44 C 54 44, 64 48, 64 66"
          fill="#17324D"
          stroke="#0E3429"
          strokeWidth="2.2"
        />
        {/* Verification Check Badge */}
        <circle cx="62" cy="20" r="10" fill="#16B65B" stroke="#FFFFFF" strokeWidth="2.2" />
        <path d="M 58 20 L 61 23 L 67 17" stroke="#FFFFFF" strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round" />
      </g>

      {/* Precision Settings Gear Cog (as in reference image) */}
      <g transform="translate(322, 90)">
        <circle cx="16" cy="16" r="14" fill="#355C7D" stroke="#0E3429" strokeWidth="2.4" />
        <circle cx="16" cy="16" r="6" fill="#FFFFFF" stroke="#0E3429" strokeWidth="1.8" />
        {/* Gear Teeth */}
        {[0, 45, 90, 135, 180, 225, 270, 315].map((angle) => (
          <rect
            key={angle}
            x="14"
            y="-1"
            width="4"
            height="5"
            rx="1"
            fill="#355C7D"
            stroke="#0E3429"
            strokeWidth="1.4"
            transform={`rotate(${angle} 16 16)`}
          />
        ))}
      </g>

      {/* Digital Key Emblem */}
      <g transform="translate(145, 48)" filter="url(#profDropShadow)">
        <circle cx="16" cy="16" r="14" stroke="#0E3429" strokeWidth="2.6" fill="#FEF3C7" />
        <circle cx="16" cy="16" r="6" stroke="#0E3429" strokeWidth="2" fill="#FFFFFF" />
        <line x1="30" y1="16" x2="72" y2="16" stroke="#0E3429" strokeWidth="3.2" strokeLinecap="round" />
        <line x1="54" y1="16" x2="54" y2="28" stroke="#0E3429" strokeWidth="2.8" strokeLinecap="round" />
        <line x1="66" y1="16" x2="66" y2="28" stroke="#0E3429" strokeWidth="2.8" strokeLinecap="round" />
      </g>
    </svg>
  );
}

/**
 * Returns the matching SVG art component for a given theme name
 */
export function HeroThemeArt({ theme }) {
  switch (theme) {
    case "finance":
      return <FinanceHeroArt />;
    case "customers":
      return <CustomersHeroArt />;
    case "orders":
      return <OrdersHeroArt />;
    case "notes":
      return <NotesHeroArt />;
    case "notifications":
      return <NotificationsHeroArt />;
    case "profile":
      return <ProfileHeroArt />;
    case "dashboard":
    default:
      return <DashboardHeroArt />;
  }
}
