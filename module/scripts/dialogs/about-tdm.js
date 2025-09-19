function AboutTDM() {
    this.launch();
}

AboutTDM = function () {
    const frame = $(DOM.loadHTML("istex-ws-extension", "scripts/dialogs/about-tdm.html"));
    const elmts = this.elmts = DOM.bind(frame);

    elmts.aboutTdmContainer.text($.i18n("istex-tdm-extension/about-header"));
    elmts.aboutTdmText.text($.i18n("istex-tdm-extension/about-content"));

    elmts.closeButton.text($.i18n("istex-tdm-extension/close-button"));

    let level = DialogSystem.showDialog(frame);

    elmts.closeButton.on('click', () => {
        DialogSystem.dismissUntil(level - 1);
    });
}
