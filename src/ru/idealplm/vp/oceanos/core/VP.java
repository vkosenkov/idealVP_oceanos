package ru.idealplm.vp.oceanos.core;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

import com.teamcenter.rac.kernel.TCComponent;
import com.teamcenter.rac.kernel.TCComponentBOMLine;
import com.teamcenter.rac.kernel.TCComponentItem;
import com.teamcenter.rac.kernel.TCComponentItemRevision;
import com.teamcenter.rac.kernel.TCException;
import com.teamcenter.rac.kernel.TCPreferenceService;
import com.teamcenter.rac.pse.plugin.Activator;

import ru.idealplm.vp.oceanos.core.DataReader;
import ru.idealplm.vp.oceanos.core.Report;
import ru.idealplm.vp.oceanos.core.Report.ReportType;
import ru.idealplm.vp.oceanos.handlers.VPHandler;
import ru.idealplm.vp.oceanos.xml.ExcelReportBuilder;
import ru.idealplm.vp.oceanos.xml.PDFReportBuilder;
import ru.idealplm.vp.oceanos.xml.PDFReportBuilderConfiguration;
import ru.idealplm.vp.oceanos.xml.XmlBuilder;
import ru.idealplm.vp.oceanos.xml.XmlBuilderConfiguration;

public class VP
{
	public static final String CLIENT_ID = UUID.randomUUID().toString();
	public enum Section {DOCS, COMPLEXES, ASSEMBLIES, DETAILS, STANDARTS, RESTS, MATERIALS, KITS}
	public enum Statuses {RELEASED}
	public static TCComponentBOMLine topBOMLine;
	public static TCComponentItem topBOMLineI;
	public static TCComponentItemRevision topBOMLineIR;
	public static TCComponentItemRevision vpIR;
	public static TCComponent generalNoteForm;
	
	public static ErrorList errorList;
	public Report report;
	private DataReader dataReader;
	
	public static TCPreferenceService preferenceService = VPHandler.session.getPreferenceService();
	
	public VP()
	{
		errorList = new ErrorList();
		report = new Report();
		report.type = ReportType.PDF;
	}
	
	public void init()
	{
		try{
			VPSettings.reset();
			topBOMLine = Activator.getPSEService().getTopBOMLine();
			topBOMLineI = topBOMLine.getItem();
			topBOMLineIR = topBOMLine.getItemRevision();
			report.targetId = topBOMLineI.getProperty("item_id");
			dataReader = new DataReader(this);
			
			String[] emptyValues = {};
			VPSettings.nonbreakableWords = preferenceService.getStringArray(preferenceService.TC_preference_site, "Oc9_Spec_NonbreakableWords", emptyValues);
		} catch (TCException ex) {
			ex.printStackTrace();
			throw new RuntimeException("Error while initializing");
		}
	}
	
	public void readExistingData()
	{
		System.out.println("@@@ READING EXISTING DATA @@@");
		dataReader.readExistingData();
	}
	
	public void readData()
	{
		System.out.println("@@@ READING DATA @@@");
		dataReader.readData();
	}

	public void buildXmlFile()
	{
		System.out.println("@@@ BUILDING XML FILE @@@");
		XmlBuilderConfiguration A3xmlBuilderConfiguration = new XmlBuilderConfiguration(23, 29);
		XmlBuilderConfiguration.MaxWidthGlobalRemark = 474;

		XmlBuilder xmlBuilder = new XmlBuilder(A3xmlBuilderConfiguration, report);
		File data = xmlBuilder.buildXml();

		report.data = data;
	}
	
	public void prepareData()
	{
		System.out.println("@@@ PREPARING DATA @@@");
	}

	public void buildReportFile()
	{
		if(report.type == ReportType.PDF)
		{
			buildXmlFile();
			InputStream template = VP.class.getResourceAsStream("/template/OceanosVPPDFTemplate.xsl");
			InputStream config = VP.class.getResourceAsStream("/template/OceanosVPUserconfig.xml");
			PDFReportBuilderConfiguration A3pdfBuilderconfiguration = new PDFReportBuilderConfiguration(template, config);
	
			report.configuration = A3pdfBuilderconfiguration;
	
			System.out.println("@@@ BUILDING PDF REPORT @@@");
			PDFReportBuilder reportBuilder = new PDFReportBuilder(report);
			reportBuilder.buildReportStatic();
		}
		else
		{
			System.out.println("@@@ BUILDING EXCEL REPORT @@@");
			ExcelReportBuilder reportBuilder = new ExcelReportBuilder(report);
			reportBuilder.buildReport();
		}
	}
	
	public void uploadReportFile()
	{
		if(report.type == ReportType.PDF)
		{
			ReportUploader uploader = new ReportUploader(this);
			uploader.addToTeamcenter();
		}
	}
	
	public void openReportFile()
	{
		try
		{
			Desktop.getDesktop().open(new File(report.report.getAbsolutePath()));
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
