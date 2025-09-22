package org.openrefine.extensions.istexTdm.cmd;

import com.google.refine.browsing.EngineConfig;
import com.google.refine.commands.EngineDependentCommand;
import com.google.refine.model.AbstractOperation;
import com.google.refine.model.Project;
import org.openrefine.extensions.istexTdm.operations.ColumnAdditionByEnrichmentOperation;

import javax.servlet.http.HttpServletRequest;

public class AddColumnByEnrichmentCommand extends EngineDependentCommand {

    @Override
    protected AbstractOperation createOperation(Project project,
            HttpServletRequest request, EngineConfig engineConfig) throws Exception {

        String baseColumnName = request.getParameter("baseColumnName");
        String columnAction = request.getParameter("columnAction");
        String newColumnName = request.getParameter("newColumnName");
        int columnInsertIndex = Integer.parseInt(request.getParameter("columnInsertIndex"));
        int delay = Integer.parseInt(request.getParameter("delay"));
        String serviceUrl = request.getParameter("serviceUrl");
                    int batchSize = Integer.parseInt(request.getParameter("batchSize"));
                    int timeout = Integer.parseInt(request.getParameter("timeout"));
        
                    return new ColumnAdditionByEnrichmentOperation(
                            engineConfig,
                            baseColumnName,
                            columnAction,
                            newColumnName,
                            columnInsertIndex,
                            delay,
                            serviceUrl,
                            batchSize,
                            timeout);    }
}
