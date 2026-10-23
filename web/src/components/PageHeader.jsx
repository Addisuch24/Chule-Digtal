import React, { useMemo } from "react";
import "./PageHeader.css";
import { HeroThemeArt } from "./HeroThemeArt";

/**
 * Animated Title Component
 * Splits title into words and characters with staggered CSS entrance variables.
 * Memoized so parent state re-renders (socket updates, form typing, list refreshes)
 * never restart or flash the animation.
 */
const AnimatedTitle = React.memo(function AnimatedTitle({ title, className = "hero-title" }) {
  const wordTokens = useMemo(() => {
    if (!title || typeof title !== "string") return [];
    const words = title.split(" ");
    let offset = 0;
    return words.map((word) => {
      // Correctly handles Amharic and Unicode graphemes
      const characters = Array.from(word);
      const start = offset;
      offset += characters.length + 1; // account for space
      return { characters, start };
    });
  }, [title]);

  if (!title) return null;
  if (typeof title !== "string") {
    return <h1 className={className}>{title}</h1>;
  }

  return (
    <h1 className={className} aria-label={title}>
      {wordTokens.map((token, wordIndex) => (
        <span key={`w-${wordIndex}`} className="hero-title-word">
          {token.characters.map((char, charIndex) => (
            <span
              key={`c-${charIndex}`}
              className="hero-title-char"
              style={{ "--char-i": token.start + charIndex }}
            >
              {char}
            </span>
          ))}
          {wordIndex < wordTokens.length - 1 && (
            <span className="hero-title-space">&nbsp;</span>
          )}
        </span>
      ))}
    </h1>
  );
});

function detectTheme(title = "", eyebrow = "") {
  const combined = `${title} ${eyebrow}`.toLowerCase();
  if (combined.includes("ፋይናንስ") || combined.includes("finance") || combined.includes("ገንዘብ")) {
    return "finance";
  }
  if (combined.includes("order") || combined.includes("ትዕዛዝ")) {
    return "orders";
  }
  if (combined.includes("ማስታወሻ") || combined.includes("note")) {
    return "notes";
  }
  if (combined.includes("customer") || combined.includes("ደንበኛ")) {
    return "customers";
  }
  if (combined.includes("notification") || combined.includes("ማሳወቂያ") || combined.includes("update")) {
    return "notifications";
  }
  if (combined.includes("መገለጫ") || combined.includes("profile") || combined.includes("account")) {
    return "profile";
  }
  return "dashboard";
}

function PageHeader({ eyebrow, title, description, translation, action, theme }) {
  const effectiveTheme = useMemo(() => {
    return theme || detectTheme(title, eyebrow);
  }, [theme, title, eyebrow]);

  return (
    <div className={`page-heading hero-header hero-theme-${effectiveTheme}`}>
      {/* Background Decorative Ambient Layers */}
      <div className="hero-header-bg" aria-hidden="true">
        <svg
          className="hero-bg-wave"
          viewBox="0 0 700 160"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          preserveAspectRatio="none"
        >
          <path
            d="M 160 0 C 240 75, 320 138, 450 96 C 550 64, 630 120, 700 84 L 700 0 Z"
            fill="var(--hero-wave-color-1, rgba(23, 50, 77, 0.05))"
          />
          <path
            d="M 280 0 C 360 80, 440 132, 540 88 C 610 60, 660 98, 700 72 L 700 0 Z"
            fill="var(--hero-wave-color-2, rgba(21, 150, 106, 0.04))"
          />
        </svg>
        <div className="hero-bg-ring hero-bg-ring-1" />
        <div className="hero-bg-ring hero-bg-ring-2" />
        <div className="hero-bg-glow hero-bg-glow-right" />
        <div className="hero-bg-glow hero-bg-glow-left" />
        <div className="hero-bg-art">
          <HeroThemeArt theme={effectiveTheme} />
        </div>
      </div>

      {/* Foreground Content */}
      <div className="hero-header-content">
        {eyebrow && (
          <div className="hero-eyebrow">
            <span className="hero-eyebrow-dot" aria-hidden="true" />
            <span>{eyebrow}</span>
          </div>
        )}

        {title && <AnimatedTitle title={title} />}

        {description && <p className="hero-description">{description}</p>}

        {translation && (
          <p className="hero-translation" lang="am">
            {translation}
          </p>
        )}
      </div>

      {/* Action / Controls Slot */}
      {action && <div className="hero-header-action">{action}</div>}
    </div>
  );
}

export default PageHeader;
