package org.fbme.lib.iec61131.service

import org.fbme.lib.iec61131.model.OldStandardXml

class InterfaceService(
    private val xmlInterface: OldStandardXml.Interface,
) {

    fun getInputVariables(): List<String> {
        return getVarNames(xmlInterface.inputVars)
    }

    fun getInOutVariables(): List<String> {
        return getVarNames(xmlInterface.inOutVars)
    }

    fun getOutputVariables(): List<String> {
        return getVarNames(xmlInterface.outputVars)
    }

    private fun getVarNames(varListList: List<OldStandardXml.VariableList>): List<String> {
        return varListList.map { varList -> varList.variableList.map { it.name } }.flatten()
    }
}