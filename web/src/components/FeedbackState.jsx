function FeedbackState({ type = "empty", title, message, action }) {
  return (
    <div className={`feedback-state feedback-${type}`} role={type === "error" ? "alert" : undefined}>
      <span className="feedback-mark" aria-hidden="true">
        {type === "error" ? "!" : type === "loading" ? "..." : "-"}
      </span>
      <div>
        <strong>{title}</strong>
        {message && <p>{message}</p>}
        {action && <div className="feedback-action">{action}</div>}
      </div>
    </div>
  );
}

export default FeedbackState;
