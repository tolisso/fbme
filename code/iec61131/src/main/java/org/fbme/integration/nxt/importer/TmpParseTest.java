package org.fbme.integration.nxt.importer;

import org.fbme.lib.iec61131.converter.*;
import org.fbme.lib.iec61131.model.OldStandardXml;
import org.fbme.lib.common.Declaration;
import org.fbme.lib.iec61499.IEC61499Factory;
import org.fbme.lib.st.STFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class TmpParseTest {
    public static List<Declaration> test(IEC61499Factory factory, STFactory stFactory, String path) throws ParserConfigurationException, IOException, SAXException {
        var project = OldStandardXml.Companion.serializeProject(path);
        var blockInterfaceService = new BlocksInterfaceInfo(project.getTypes().getPous());
        var converterArguments = new ConverterArguments(factory, stFactory, blockInterfaceService);
        var resDeclarations = new ArrayList<>(getChildNodes(converterArguments, project));
        resDeclarations.add(new SystemConverter(project, converterArguments).createSystem());
        return resDeclarations;
    }

    private static List<Declaration> getChildNodes(
            ConverterArguments converterArguments,
            OldStandardXml.Project project
    ) {
        return project.getTypes().getPous().getPouList().stream()
                .map(xmlPou -> {
                    var result = new ArrayList<Declaration>();
                    var networkFbtd = converterArguments.getFactory().createCompositeFBTypeDeclaration(null);
                    result.addAll(new FbNetworkConverter(
                            xmlPou.getBodyList().get(0).getFbd(),
                            xmlPou.getPouInterface(),
                            converterArguments,
                            "REQ",
                            "CNF"
                    ).fillNetwork(networkFbtd.getNetwork()));
                    new FbtdInterfaceConverter(xmlPou, converterArguments).fillInterface(networkFbtd);
                    result.add(networkFbtd);
                    return result;
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
