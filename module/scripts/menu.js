I18NUtil.init("istex-ws-extension");

/* Add menu to extension bar */
ExtensionBar.MenuItems.push({
  id: "istex-ws-extension-configure",
  label: $.i18n('istex-ws-extension/menu-label'),
  submenu: [
    {
      id: "istex-ws-extension/configuration",
      label: $.i18n('istex-ws-extension/manage-ws'),
      click: function () {
        new ManageLLMSettingsUI();
      }
    }
  ]
});

/* Add submenu to column header menu */
DataTableColumnHeaderUI.extendMenu(function (column, columnHeaderUI, menu) {
  MenuSystem.appendTo(menu, "", [
    { /* separator */ },
    {
      id: "istex-ws-extension/enrichment",
      label: $.i18n('istex-ws-extension/enrichment'),
      click: function () {
        new TDMEnrichmentDialog(column);
      }
    },
  ]);
});
