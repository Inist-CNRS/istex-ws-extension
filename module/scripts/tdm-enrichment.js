function TDMEnrichmentDialog(column) {
    this.launch(column)
}

TDMEnrichmentDialog.prototype.launch = function (column) {
    const columnIndex = Refine.columnNameToColumnIndex(column.name);
    const frame = $(
        DOM.loadHTML("istex-ws-extension", "scripts/dialogs/tdm-enrichment.html")
    );

    const elmts = DOM.bind(frame);
    this._elmts = elmts;
    let serviceUrl = "";

    elmts.columnNameLabel.text($.i18n('tdm-enrichment/column-name-label'));
    elmts.serviceUrlLabel.text($.i18n('tdm-enrichment/service-url-label'));

    elmts.dialogHeader.text($.i18n('tdm-enrichment/dialog-title') + column.name);
    // SEE llm-chatcompletion.js #18

    elmts.okButton.html($.i18n('core-buttons/ok'));
    elmts.cancelButton.text($.i18n('core-buttons/cancel'));

    const colActionSelector = elmts.columnaction;
    colActionSelector.append('<option value="' + "add" + '">' + $.i18n("tdm-enrichment/col-mode-add") + '</option>');
    colActionSelector.append('<option value="' + "update" + '">' + $.i18n("tdm-enrichment/col-mode-upd") + '</option>');

    const level = DialogSystem.showDialog(frame);
    const dismiss = function () { DialogSystem.dismissUntil(level - 1); };

    elmts.cancelButton.on('click', dismiss);

    elmts.okButton.on('click', function (event) {
        const colActionSelector = elmts.columnaction;
        const columnAction = colActionSelector.val();

        event.preventDefault();

        const columnName = jQueryTrim(elmts.columnNameInput[0].value);
        if (!columnName.length) {
            alert($.i18n('tdm-enrichment/col-name-warning'));
            return;
        }

        serviceUrl = jQueryTrim(elmts.serviceUrlInput.val());
        if (!serviceUrl.length) {
            alert($.i18n('tdm-enrichment/service-url-warning'));
            return;
        }

        let delay = 1;

        Refine.postProcess(
            "istex-ws-extension",
            "add-column-by-enrichment",
            {},
            {
                baseColumnName: column.name,
                columnAction: columnAction,
                newColumnName: columnName,
                columnInsertIndex: columnIndex + 1,
                delay: delay,
                serviceUrl: serviceUrl
            },
            { includeEngine: true, modelsChanged: true, cellsChanged: true, columnStatsChanged: true, rowIdsPreserved: true, recordIdsPreserved: true }
        );

        dismiss();
    });

    this.initTabs();
};

TDMEnrichmentDialog.prototype.initTabs = function () {
    const self = this;

    $('#tdm-enrichment-tabs').tabs({
        activate: function (event, ui) {
            const selectedTabId = ui.newPanel.attr('id');

            if (selectedTabId === 'tdm-enrichment-tab-1') {
                self._renderForm()
            }
        }
    });

    self._renderForm();
};

TDMEnrichmentDialog.prototype._renderForm = function () {
    const self = this;

    // labels
    const _serviceUrl = $.i18n('tdm-enrichment/service-url');
    const _urlPlaceholder = $.i18n('tdm-enrichment/url-placeholder');

    // const container = this._elmts.dialogBody.empty();
    const container = this._elmts.dialogBody;
    // container.serviceUrlInputId.attr('placeholder', _urlPlaceholder);
    // const paragraph = $('<p>').appendTo(container);
};
