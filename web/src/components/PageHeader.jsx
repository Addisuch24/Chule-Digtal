function PageHeader({ eyebrow, title, description, translation, action }) {
  return (
    <div className="page-heading">
      <div>
        {eyebrow && <p className="eyebrow">{eyebrow}</p>}
        {title && <h1>{title}</h1>}
        {description && <p className="page-description">{description}</p>}
        {translation && (
          <p className="page-description page-description-translation" lang="am">
            {translation}
          </p>
        )}
      </div>
      {action && <div className="page-heading-action">{action}</div>}
    </div>
  );
}

export default PageHeader;
