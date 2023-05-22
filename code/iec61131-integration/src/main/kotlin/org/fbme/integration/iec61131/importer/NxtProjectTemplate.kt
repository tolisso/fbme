package org.fbme.integration.iec61131.importer

import jetbrains.mps.project.AbstractModule
import jetbrains.mps.smodel.ModelImports
import org.fbme.ide.iec61499.repository.PlatformElement
import org.fbme.ide.iec61499.repository.PlatformElementsOwner
import org.fbme.ide.iec61499.repository.PlatformRepository
import org.fbme.ide.platform.projectWizard.Iec61499ProjectTemplate
import org.fbme.integration.nxt.importer.TmpParseTest
import org.jetbrains.mps.openapi.model.SModel
import org.jetbrains.mps.openapi.model.SNode
import org.jetbrains.mps.openapi.persistence.PersistenceFacade
import java.util.*


class NxtImportProjectTemplate : Iec61499ProjectTemplate(
    NxtImportSystemConfigSolutionSettings("NewModel"),
    "Import IEC61131 project",
    "Import project from IEC61131 file",
    NxtIntegrationIcons.importProject,
    null
) {
    override fun initModel(repository: PlatformRepository, model: SModel): PlatformElement {
        val settings = settings as NxtImportSystemConfigSolutionSettings
        val iec61131ProjectLocation = settings.getNxtImportLocation()
        addModelImports(model)

        readModel(iec61131ProjectLocation, model)
        val first = model.rootNodes.first()

        return repository.getAdapter(first, PlatformElement::class.java)
    }

    private fun addModelImports(model: SModel) {
        val iec61499ModuleId = "b8a7e14f-52ea-4ee2-b17e-26c27da8084c(IEC-61499)"
        (model.module as AbstractModule).addDependency(
            PersistenceFacade.getInstance().createModuleReference(iec61499ModuleId), false
        )

        val stdLibModuleId = "r:fa98296a-e4fa-4f84-b917-968f5f770c4b(iec61499.4diac.stdlib)"
        ModelImports(model).addModelImport(PersistenceFacade.getInstance().createModelReference(stdLibModuleId))
    }


    private fun readModel(path: String, model: SModel) {
        getRootNodes(path).forEach { model.addRootNode(it) }
    }

    private fun getRootNodes(nxtImportFile: String): List<SNode> {
        val owner = PlatformElementsOwner();
        val configuration = NxtImporterConfiguration.FACTORY.createConfiguration(owner);

        val resultNodes = ArrayList<SNode>();

        for (fbtd in TmpParseTest.test(configuration.entryFactory, configuration.stEntryFactory, nxtImportFile)) {
            resultNodes.add((fbtd as PlatformElement).node);
        }
        return resultNodes;
    }
}
