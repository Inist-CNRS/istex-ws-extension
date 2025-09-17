package org.openrefine.extensions.istexTdm.operations;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.browsing.Engine;
import com.google.refine.browsing.EngineConfig;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.expr.*;
import com.google.refine.history.Change;
import com.google.refine.history.HistoryEntry;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.model.changes.*;
import com.google.refine.operations.EngineDependentOperation;
import com.google.refine.process.LongRunningProcess;
import com.google.refine.process.Process;
import org.apache.commons.lang.Validate;
import org.openrefine.extensions.istexTdm.service.EnrichmentService;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ColumnAdditionByEnrichmentOperation extends EngineDependentOperation {

    final protected String _baseColumnName;
    final protected String _columnAction;
    final protected String _newColumnName;
    final protected int _columnInsertIndex;
    final protected int _delay;
    final protected String _serviceUrl;

    @JsonCreator
    public ColumnAdditionByEnrichmentOperation(
            @JsonProperty("engineConfig") EngineConfig engineConfig,
            @JsonProperty("baseColumnName") String baseColumnName,
            @JsonProperty("columnAction") String columnAction,
            @JsonProperty("newColumnName") String newColumnName,
            @JsonProperty("columnInsertIndex") int columnInsertIndex,
            @JsonProperty("delay") int delay,
            @JsonProperty("serviceUrl") String serviceUrl) {
        super(engineConfig);

        _baseColumnName = baseColumnName;
        _columnAction = columnAction;
        _newColumnName = newColumnName;
        _columnInsertIndex = columnInsertIndex;
        _delay = delay;
        _serviceUrl = serviceUrl;
    }

    public void validate() {
        Validate.notNull(_baseColumnName, "Missing base column name");
        Validate.notNull(_columnAction, "Missing column action");
        Validate.notNull(_newColumnName, "Missing column name");
        Validate.isTrue(_columnInsertIndex >= 0, "Invalid column insert index");
        Validate.notNull(_serviceUrl, "Missing service url");
    }

    @JsonProperty("columnAction")
    public String get_columnAction() {
        return _columnAction;
    }

    @JsonProperty("newColumnName")
    public String getNewColumnName() {
        return _newColumnName;
    }

    @JsonProperty("columnInsertIndex")
    public int getColumnInsertIndex() {
        return _columnInsertIndex;
    }

    @JsonProperty("baseColumnName")
    public String getBaseColumnName() {
        return _baseColumnName;
    }

    @JsonProperty("serviceUrl")
    public String getServiceUrl() {
        return _serviceUrl;
    }

    @Override
    protected String getBriefDescription(Project project) {
        String description = _columnAction.equals("add")
                ? "Create column {0} at index {1} by TDM enrichment process on column {2}"
                : "Update column {0} by TDM enrichment process on column {2}";
        return MessageFormat.format(description, _newColumnName, _columnInsertIndex, _baseColumnName);
    }

    protected String createDescription(Column column, List<CellAtRow> cellsAtRows) {
        return MessageFormat.format(
                "{0} column {1}, filling {2} rows by TDM enrichment process based on column {3} using {4}",
                _columnAction.toUpperCase(), _newColumnName, cellsAtRows.size(), column.getName(), _serviceUrl);
    }

    @Override
    public Process createProcess(Project project, Properties options) throws Exception {
        Engine engine = createEngine(project);
        engine.initializeFromConfig(_engineConfig);

        return new ColumnAdditionByEnrichmentProcess(
                project,
                engine,
                getBriefDescription(null));
    }

    public class ColumnAdditionByEnrichmentProcess extends LongRunningProcess implements Runnable {

        final protected Project _project;
        final protected Engine _engine;
        final protected long _historyEntryID;
        // final protected LLMConfiguration _llmConfiguration;

        public ColumnAdditionByEnrichmentProcess(
                Project project,
                Engine engine,
                String description) {
            super(description);
            _project = project;
            _engine = engine;
            _historyEntryID = HistoryEntry.allocateID();
            // _llmConfiguration = LLMUtils.getLLMProvider(_providerLabel);
        }

        @Override
        protected Runnable getRunnable() {
            return this;
        }

        @Override
        public void run() {
            Column column = _project.columnModel.getColumnByName(_baseColumnName);
            if (column == null) {
                _project.processManager.onFailedProcess(this, new Exception("No column named " + _baseColumnName));
                return;
            }
            if (_columnAction.equals("add") && _project.columnModel.getColumnByName(_newColumnName) != null) {
                _project.processManager.onFailedProcess(this, new Exception(
                        "Column name is not unique. Another column with same name exists " + _newColumnName));
                return;
            }

            if (_columnAction.equals("update") && _project.columnModel.getColumnByName(_newColumnName) == null) {
                _project.processManager.onFailedProcess(this, new Exception("Column does not exist " + _newColumnName));
                return;
            }

            List<CellAtRow> dataRows = new ArrayList<CellAtRow>(_project.rows.size());

            FilteredRows filteredRows = _engine.getAllFilteredRows();
            filteredRows.accept(_project, createRowVisitor(dataRows));

            int count = dataRows.size();
            List<CellAtRow> responseBodies = new ArrayList<CellAtRow>(count);
            Change columnDataChange = null;
            int i = 0;
            for (CellAtRow rowData : dataRows) {
                String userData = rowData.cell.value.toString();

                Serializable response = null;
                response = fetch(userData);
                if (response != null) {
                    CellAtRow cellAtRow = new CellAtRow(
                            rowData.row,
                            new Cell(response, null));

                    responseBodies.add(cellAtRow);
                }
                _progress = i++ * 100 / count;
                if (_canceled) {
                    break;
                }
                // if (_llmConfiguration.getWaitTime() > 0) {
                // try {
                // Thread.sleep(_llmConfiguration.getWaitTime());
                // } catch (InterruptedException e) {
                // // do nothing
                // }
                // }
            }

            if (!_canceled) {
                if (_columnAction.equals("update")) {
                    Column resultsColumn = _project.columnModel.getColumnByName(_newColumnName);
                    int resultsCellIndex = resultsColumn != null ? resultsColumn.getCellIndex() : -1;
                    // column already exists, we overwrite cells where we made edits
                    CellChange[] cellChanges = new CellChange[count];
                    i = 0;
                    for (CellAtRow dataCell : responseBodies) {
                        int rowId = dataCell.row;
                        Cell oldCell = _project.rows.get(rowId).getCell(resultsCellIndex);
                        cellChanges[i] = new CellChange(rowId, resultsCellIndex,
                                oldCell, dataCell.cell);
                        i++;
                    }
                    columnDataChange = new MassCellChange(cellChanges, _newColumnName, false);
                } else {
                    columnDataChange = new ColumnAdditionChange(_newColumnName, _columnInsertIndex, responseBodies);
                }

                Change fullChange = new MassChange(Arrays.asList(columnDataChange), false);

                HistoryEntry historyEntry = new HistoryEntry(
                        _historyEntryID,
                        _project,
                        _description,
                        ColumnAdditionByEnrichmentOperation.this,
                        fullChange);

                _project.history.addEntry(historyEntry);
                _project.processManager.onDoneProcess(this);
            }
        }

        RowVisitor createRowVisitor(List<CellAtRow> cellsAtRows) {
            return new RowVisitor() {

                int cellIndex;
                Properties bindings;
                List<CellAtRow> cellsAtRows;

                public RowVisitor init(List<CellAtRow> cellsAtRows) {
                    Column column = _project.columnModel.getColumnByName(_baseColumnName);

                    this.cellIndex = column.getCellIndex();
                    this.bindings = ExpressionUtils.createBindings(_project);
                    this.cellsAtRows = cellsAtRows;
                    return this;
                }

                @Override
                public void start(Project project) {
                    // nothing to do
                }

                @Override
                public void end(Project project) {
                    // nothing to do
                }

                @Override
                public boolean visit(Project project, int rowIndex, Row row) {
                    Cell cell = row.getCell(cellIndex);
                    Cell newCell = null;

                    newCell = (Cell) cell;
                    if (newCell != null) {
                        cellsAtRows.add(new CellAtRow(rowIndex, newCell));
                    }

                    return false;
                }
            }.init(cellsAtRows);
        }

        Serializable fetch(String userContent) {
            try {
                return EnrichmentService.invoke(_serviceUrl, userContent);
            } catch (Exception e) {
                return e.getMessage();
            }
        }
    }

}
