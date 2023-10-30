package org.nrg.xnat.turbine.modules.screens;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Map;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.RadRadiologyreaddata;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrqcscandata;
import org.nrg.xdat.om.XnatMrscandata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.om.XnatPetqcscandata;
import org.nrg.xdat.om.XnatPetscandata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatQcmanualassessordata;
import org.nrg.xdat.om.XnatQcscandata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.EditScreenA;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.ItemI;
import org.nrg.xft.XFTItem;
import org.nrg.xft.exception.ElementNotFoundException;
import org.nrg.xft.exception.FieldNotFoundException;
import org.nrg.xft.exception.XFTInitException;
import org.nrg.xft.schema.Wrappers.GenericWrapper.GenericWrapperElement;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.StringUtils;
import org.nrg.xnat.turbine.utils.XNATUtils;

/**
 * @author XDAT
 *
 */
public class XDATScreen_edit_rad_radiologyReadData extends EditScreenA {
	static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getLogger(XDATScreen_edit_rad_radiologyReadData.class);
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.EditScreenA#getElementName()
	 */
	public String getElementName() {
	    return "rad:radiologyReadData";
	}
	
	public ItemI getEmptyItem(RunData data) throws Exception
	{
		final UserI user = TurbineUtils.getUser(data);
		final RadRadiologyreaddata radRead = new RadRadiologyreaddata(XFTItem.NewItem(getElementName(), user));
		final String search_element = TurbineUtils.GetSearchElement(data);
		if (!StringUtils.IsEmpty(search_element)) {
			final GenericWrapperElement se = GenericWrapperElement.GetElement(search_element);
			if (se.instanceOf(XnatImagesessiondata.SCHEMA_ELEMENT_NAME)) {
				final String search_value = ((String)org.nrg.xdat.turbine.utils.TurbineUtils.GetPassedParameter("search_value",data));
				if (!StringUtils.IsEmpty(search_value)) {
					XnatImagesessiondata imageSession = new XnatImagesessiondata(TurbineUtils.GetItemBySearch(data));

					radRead.setImagesessionId(search_value);
					radRead.setId(XnatExperimentdata.CreateNewID());
					radRead.setLabel(imageSession.getLabel() + "_RAD_"+ Calendar.getInstance().getTimeInMillis());
					radRead.setProject(imageSession.getProject());

				}
			}
		}

		return radRead.getItem();
	}
	/* (non-Javadoc)
	 * @see org.nrg.xdat.turbine.modules.screens.SecureReport#finalProcessing(org.apache.turbine.util.RunData, org.apache.velocity.context.Context)
	 */
	public void finalProcessing(RunData data, Context context) {
	    Hashtable hash = XNATUtils.getInvestigatorsForCreate(getElementName(),data);
        context.put("investigators",hash);

        XDATUser user = TurbineUtils.getUser(data);
        try {        	
            if (data.getParameters().get("mrsession")!=null){
                if (item.getProperty("imageSession_ID")==null){
                    item.setProperty("imageSession_ID", data.getParameters().get("mrsession"));
                }
            }
            
            if (item.getProperty("reader")==null){
                String n ="";
                if (user.getFirstname()!=null){
                    n += user.getFirstname().substring(0,1) + ". ";
                }
                
                n += user.getLastname();
                item.setProperty("reader", n);
            }
            
            XnatProjectdata proj=null;
            RadRadiologyreaddata rad = (RadRadiologyreaddata) context.get("om");
            XnatMrsessiondata mr = rad.getMrSessionData();
            
        	if(mr!=null){            	
        		proj=mr.getPrimaryProject(false);
        	}else{
        		 if(data.getParameters().getString("project")!=null){
                 	proj=XnatProjectdata.getXnatProjectdatasById(data.getParameters().getString("project"), user, false);
                 }
        	}
                        
            if (item.getProperty("history")==null){
                if(mr!=null){
                    item.setProperty("history",mr.getSubjectData().getCohort());
                }
            }
            /*
            if (item.getProperty("technique")==null){
                if(mr!=null){
                    String tech = "";
                    Hashtable<String,Integer> counts = new Hashtable<String,Integer>();
                    
                    for(int i=0;i<mr.getScans_scan().size();i++){
                        XnatImagescandata scan = (XnatImagescandata)mr.getScans_scan().get(i);
                        if(scan.getType()!=null){
                            if (counts.get(scan.getType())==null){
                                counts.put(scan.getType(), new Integer(1)); 
                            }else{
                                counts.put(scan.getType(),counts.get(scan.getType())+1);
                            }
                        }
                    }
                    
                    for(Map.Entry<String,Integer> entry: counts.entrySet()){
                        if (!tech.equals(""))tech +=", ";
                        tech +="" + entry.getValue() + " " + entry.getKey();
                    }
                    
                    item.setProperty("technique", tech);
                }
            }*/
            
            long timestamp=Calendar.getInstance().getTimeInMillis();
            context.put("timestamp", timestamp);
            
            if (item.getProperty("date")==null){
                item.setProperty("date", Calendar.getInstance().getTime());
            }
            
            if(rad.getId()==null){
            	String s = mr.getIdentifier(proj.getId());
            	rad.setId(mr.getId() + "_RAD_" + timestamp);
            	rad.setLabel(s + "_RAD_" + timestamp);
            	rad.setProject(proj.getId());
            }
            
            
        } catch (XFTInitException e) {
            logger.error("",e);
        } catch (ElementNotFoundException e) {
            logger.error("",e);
        } catch (FieldNotFoundException e) {
            logger.error("",e);
        } catch (Exception e) {
            logger.error("",e);
        }
	}}