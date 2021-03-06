package com.dotmarketing.portlets.htmlpageasset.business;

import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.exception.ResourceNotFoundException;

import com.dotcms.enterprise.LicenseUtil;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.beans.MultiTree;
import com.dotmarketing.beans.Permission;
import com.dotmarketing.beans.UserProxy;
import com.dotmarketing.beans.VersionInfo;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.web.LanguageWebAPI;
import com.dotmarketing.business.web.WebAPILocator;
import com.dotmarketing.cache.LiveCache;
import com.dotmarketing.cache.StructureCache;
import com.dotmarketing.cache.WorkingCache;
import com.dotmarketing.cmis.proxy.DotInvocationHandler;
import com.dotmarketing.cmis.proxy.DotRequestProxy;
import com.dotmarketing.cmis.proxy.DotResponseProxy;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.db.DbConnectionFactory;
import com.dotmarketing.db.HibernateUtil;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.factories.MultiTreeFactory;
import com.dotmarketing.filters.ClickstreamFilter;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPIImpl;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.structure.factories.FieldFactory;
import com.dotmarketing.portlets.structure.factories.StructureFactory;
import com.dotmarketing.portlets.structure.model.Field;
import com.dotmarketing.portlets.structure.model.Structure;
import com.dotmarketing.portlets.templates.model.Template;
import com.dotmarketing.services.PageServices;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.CookieUtil;
import com.dotmarketing.util.InodeUtils;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.RegEX;
import com.dotmarketing.util.UtilMethods;
import com.dotmarketing.util.VelocityUtil;
import com.dotmarketing.util.WebKeys;
import com.dotmarketing.velocity.VelocityServlet;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class HTMLPageAssetAPIImpl implements HTMLPageAssetAPI {

    public static final String DEFAULT_HTML_PAGE_ASSET_STRUCTURE_HOST_FIELD = "defaultHTMLPageAssetStructure";
    private static PermissionAPI permissionAPI = APILocator.getPermissionAPI();

    @Override
    public void createHTMLPageAssetBaseFields(Structure structure) throws DotDataException, DotStateException {
        if (structure == null || !InodeUtils.isSet(structure.getInode())) {
            throw new DotStateException("Cannot create base htmlpage asset fields on a structure that doesn't exist");
        }
        if (structure.getStructureType() != Structure.STRUCTURE_TYPE_HTMLPAGE) {
            throw new DotStateException("Cannot create base htmlpage asset fields on a structure that is not of htmlpage asset type");
        }
        
        Field field = new Field(TITLE_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, true, true, 1, "$velutil.mergeTemplate('/static/htmlpage_assets/title_custom_field.vtl')", "", "", true, false, true);
        field.setVelocityVarName(TITLE_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(HOST_FOLDER_FIELD_NAME, Field.FieldType.HOST_OR_FOLDER, Field.DataType.TEXT, structure, true, false, true, 2, "", "", "", true, false, true);
        field.setVelocityVarName(HOST_FOLDER_FIELD);
        FieldFactory.saveField(field);        
        
        field = new Field(URL_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, true, true, true, 3, "", "", "", true, false, true);
        field.setVelocityVarName(URL_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(CACHE_TTL_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, true, true, 4, 
                "$velutil.mergeTemplate('/static/htmlpage_assets/cachettl_custom_field.vtl')", "", "^[0-9]+$", true, false, true);
        field.setVelocityVarName(CACHE_TTL_FIELD);
        FieldFactory.saveField(field);
        
        
        field = new Field(TEMPLATE_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, true, false, true, 5, 
                "$velutil.mergeTemplate('/static/htmlpage_assets/template_custom_field.vtl')", "", "", true, false, true);
        field.setVelocityVarName(TEMPLATE_FIELD);
        FieldFactory.saveField(field);
        
        
        
        
        
        field = new Field(ADVANCED_PROPERTIES_TAB_NAME, Field.FieldType.TAB_DIVIDER, Field.DataType.SECTION_DIVIDER, structure, false, false, false, 6, "", "", "", false, false, false);
        field.setVelocityVarName(ADVANCED_PROPERTIES_TAB);
        FieldFactory.saveField(field);
        
        field = new Field(SHOW_ON_MENU_FIELD_NAME, Field.FieldType.CHECKBOX, Field.DataType.TEXT, structure, false, false, true, 7, "|true", "false", "", true, false, false);
        field.setVelocityVarName(SHOW_ON_MENU_FIELD);
        FieldFactory.saveField(field);

        field = new Field(SORT_ORDER_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.INTEGER, structure, true, false, true, 8, "", "0", "", true, false, true);
        field.setVelocityVarName(SORT_ORDER_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(FRIENDLY_NAME_FIELD_NAME, Field.FieldType.TEXT, Field.DataType.TEXT, structure, false, false, true, 9, "", "", "", true, false, true);
        field.setVelocityVarName(FRIENDLY_NAME_FIELD);
        FieldFactory.saveField(field);
        

        
        field = new Field(REDIRECT_URL_FIELD_NAME, Field.FieldType.CUSTOM_FIELD, Field.DataType.TEXT, structure, false, true, true, 10, 
                "$velutil.mergeTemplate('/static/htmlpage_assets/redirect_custom_field.vtl')", "", "", true, false, true);
        field.setVelocityVarName(REDIRECT_URL_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(HTTPS_REQUIRED_FIELD_NAME, Field.FieldType.CHECKBOX, Field.DataType.TEXT, structure, false, false, true, 11, "|true", "false", "", true, false, false);
        field.setVelocityVarName(HTTPS_REQUIRED_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(SEO_DESCRIPTION_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, true, 12, "", "", "", true, false, true);
        field.setVelocityVarName(SEO_DESCRIPTION_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(SEO_KEYWORDS_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, true, 13, "", "", "", true, false, true);
        field.setVelocityVarName(SEO_KEYWORDS_FIELD);
        FieldFactory.saveField(field);
        
        field = new Field(PAGE_METADATA_FIELD_NAME, Field.FieldType.TEXT_AREA, Field.DataType.LONG_TEXT, structure, false, false, true, 14, "", "", "", true, false, true);
        field.setVelocityVarName(PAGE_METADATA_FIELD);
        FieldFactory.saveField(field);
                
    }

    @Override
    public Template getTemplate(IHTMLPage page, boolean preview) throws DotDataException, DotSecurityException {
        if (preview) 
            return APILocator.getTemplateAPI().findWorkingTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
        else
            return APILocator.getTemplateAPI().findLiveTemplate(page.getTemplateId(), APILocator.getUserAPI().getSystemUser(), false);
    }

    @Override
    public Host getParentHost(IHTMLPage page) throws DotDataException, DotStateException, DotSecurityException {
        return APILocator.getHostAPI().find(APILocator.getIdentifierAPI().find(page).getHostId(), APILocator.getUserAPI().getSystemUser(), false);
    }

    @Override
    public HTMLPageAsset fromContentlet(Contentlet con) {
        if (con == null || con.getStructure().getStructureType() != Structure.STRUCTURE_TYPE_HTMLPAGE) {
            throw new DotStateException("Contentlet : " + con.getInode() + " is not a pageAsset");
        }

        HTMLPageAsset pa=new HTMLPageAsset();
        pa.setStructureInode(con.getStructureInode());
        try {
            APILocator.getContentletAPI().copyProperties((Contentlet) pa, con.getMap());
        } catch (Exception e) {
            throw new DotStateException("Page Copy Failed", e);
        }
        pa.setHost(con.getHost());
        if(UtilMethods.isSet(con.getFolder())){
            try{
                Identifier ident = APILocator.getIdentifierAPI().find(con);
                User systemUser = APILocator.getUserAPI().getSystemUser();
                Host host = APILocator.getHostAPI().find(con.getHost(), systemUser , false);
                Folder folder = APILocator.getFolderAPI().findFolderByPath(ident.getParentPath(), host, systemUser, false);
                pa.setFolder(folder.getInode());
            }catch(Exception e){
            	pa=new HTMLPageAsset();
                Logger.warn(this, "Unable to convert contentlet to page asset " + con, e);
            }
        }
        return pa;
    }

    @Override
    public List<IHTMLPage> getHTMLPages(Object parent, boolean live, boolean deleted, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
		return getHTMLPages(parent, live, deleted, -1, 0, "", user,
				respectFrontEndRoles);
    }

	@Override
	public List<IHTMLPage> getHTMLPages(Object parent, boolean live,
			boolean deleted, int limit, int offset, String sortBy, User user,
			boolean respectFrontEndRoles) throws DotDataException,
			DotSecurityException {
		List<IHTMLPage> pages = new ArrayList<IHTMLPage>();
		StringBuffer query = new StringBuffer();
		String liveWorkingDeleted = (live) ? " +live:true "
				: (deleted) ? " +working:true +deleted:true "
						: " +working:true -deleted:true";
		query.append(liveWorkingDeleted);
		if (parent instanceof Folder) {
			query.append(" +conFolder:" + ((Folder) parent).getInode());
		} else if (parent instanceof Host) {
			query.append(" +conFolder:SYSTEM_FOLDER +conHost:"
					+ ((Host) parent).getIdentifier());
		// if not a folder or host the filtering is done by template (parent) 
		}else if (parent instanceof String)
			if(!((String)parent).isEmpty()){
				// list of content types (htmlpage type)
				List<Structure> structures = StructureFactory.getStructures("structureType="+Structure.STRUCTURE_TYPE_HTMLPAGE, "", 0, 0, "");
				StringBuilder structuresList = new StringBuilder();
				boolean notOR = true;
				
				// creates a list of content types with the template field e.g. htmlpageasset.template:## OR newpages.template:##
				for(Structure structure: structures){
					if(notOR){
						notOR=!notOR;
					}else
						structuresList.append(" OR ");
					structuresList.append(structure.getVelocityVarName());
					structuresList.append(".template:");
					structuresList.append((String)parent);					
				}
				if(structuresList.length()>0)
					query.append(" +("+ structuresList.toString().trim()+")" );
			}
		
		query.append(" +structureType:" + Structure.STRUCTURE_TYPE_HTMLPAGE);
		if (!UtilMethods.isSet(sortBy)) {
			sortBy = "modDate asc";
		}
		List<Contentlet> contentlets = APILocator.getContentletAPI().search(
				query.toString(), limit, offset, sortBy, user,
				respectFrontEndRoles);
		for (Contentlet cont : contentlets) {
			if(UtilMethods.isSet(fromContentlet(cont).getInode()))
				pages.add(fromContentlet(cont));
		}
		return pages;
	}
    
    @Override
    public List<IHTMLPage> getLiveHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        return getHTMLPages(parent, true, false, user, respectFrontEndRoles);
    }

    @Override
    public List<IHTMLPage> getLiveHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException {
        return getHTMLPages( parent, true, false, user, respectFrontEndRoles );
    }

    @Override
    public List<IHTMLPage> getWorkingHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        return getHTMLPages(parent, false, false, user, respectFrontEndRoles);
    }

    @Override
    public List<IHTMLPage> getWorkingHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException {
        return getHTMLPages( parent, false, false, user, respectFrontEndRoles );
    }

    @Override
    public List<IHTMLPage> getDeletedHTMLPages(Folder parent, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException {
        return getHTMLPages(parent, false, true, user, respectFrontEndRoles);
    }

    @Override
    public List<IHTMLPage> getDeletedHTMLPages ( Host parent, User user, boolean respectFrontEndRoles ) throws DotDataException, DotSecurityException {
        return getHTMLPages( parent, false, true, user, respectFrontEndRoles );
    }

    @Override
    public Folder getParentFolder(IHTMLPage htmlPage) throws DotDataException, DotSecurityException {
        Identifier ident = APILocator.getIdentifierAPI().find(htmlPage.getIdentifier());
        if(ident.getParentPath().equals("/")) {
            return APILocator.getFolderAPI().findSystemFolder();
        }
        else {
            return APILocator.getFolderAPI().findFolderByPath(
                    ident.getParentPath(), APILocator.getHostAPI().find(
                            ident.getHostId(), APILocator.getUserAPI().getSystemUser(), false), 
                            APILocator.getUserAPI().getSystemUser(), false);
        }
    }

    protected HTMLPageAsset copyLegacyData(HTMLPage legacyPage, User user, boolean respectFrontEndPermissions) throws DotStateException, DotDataException, DotSecurityException {
        Identifier legacyident=APILocator.getIdentifierAPI().find(legacyPage);
        HTMLPageAsset newpage=new HTMLPageAsset();
        newpage.setStructureInode(getHostDefaultPageType(legacyident.getHostId()));
        newpage.setLanguageId(APILocator.getLanguageAPI().getDefaultLanguage().getId());
        newpage.setTitle(legacyPage.getTitle());
        newpage.setFriendlyName(legacyPage.getFriendlyName());
        newpage.setHttpsRequired(legacyPage.isHttpsRequired());
        newpage.setTemplateId(legacyPage.getTemplateId());
        newpage.setSeoDescription(legacyPage.getSeoDescription());
        newpage.setSeoKeywords(legacyPage.getSeoKeywords());
        newpage.setInode(legacyPage.getInode());
        newpage.setIdentifier(legacyPage.getIdentifier());
        newpage.setHost(legacyident.getHostId());
        newpage.setFolder(APILocator.getFolderAPI().findFolderByPath(
                legacyident.getParentPath(), legacyident.getHostId(), user, respectFrontEndPermissions).getInode());
        newpage.setPageUrl(legacyPage.getPageUrl());
        newpage.setCacheTTL(legacyPage.getCacheTTL());
        newpage.setMetadata(legacyPage.getMetadata());
        newpage.setSortOrder(legacyPage.getSortOrder());
        newpage.setShowOnMenu(legacyPage.isShowOnMenu());
        newpage.setModUser(legacyPage.getModUser());
        newpage.setModDate(legacyPage.getModDate());
        return newpage;
    }
    
    @Override
    public void migrateAllLegacyPages(final User user, boolean respectFrontEndPermissions) throws Exception {
    	new Thread() {
    		public void run() {
    			try {
    				int offset = 0;
    				int limit = 100;
    				List<HTMLPage> elements = APILocator.getHTMLPageAPI().findHtmlPages(APILocator.getUserAPI().getSystemUser(), true, null, null, null, null, null, offset, limit, null);

    				while(!elements.isEmpty()) {
    					int migrated = 0;

    					for (HTMLPage htmlPage : elements) {
    						if(migrated==0)
    							HibernateUtil.startTransaction();

    						migrateLegacyPage(htmlPage, user, false);

    						migrated++;

    						if(migrated==elements.size() || (migrated>0 && migrated%100==0) ) {
    							HibernateUtil.commitTransaction();
    							elements = APILocator.getHTMLPageAPI().findHtmlPages(APILocator.getUserAPI().getSystemUser(), true, null, null, null, null, null, offset+limit, limit, null);
    						}
    					}

    					}

    				//Create a new notification to inform the pages were migrated
    				APILocator.getNotificationAPI().generateNotification( LanguageUtil.get( user.getLocale(), "htmlpages-migration-finished" ), NotificationLevel.INFO, user.getUserId() );

    			}

    			catch(Exception ex) {
    				try {
                        HibernateUtil.rollbackTransaction();
                    } catch (DotHibernateException e1) {
                        Logger.warn(this, e1.getMessage(),e1);
                    }
    			}
    			finally {
    				try {
                        HibernateUtil.closeSession();
                    } catch (DotHibernateException e) {
                        Logger.error(LicenseUtil.class, "can't close session after adding to cluster",e);
                    }
    			}
    		}
    	}.start();

    }
        
    
    @Override
    public HTMLPageAsset migrateLegacyPage(HTMLPage legacyPage, User user, boolean respectFrontEndPermissions) throws Exception {
        Identifier legacyident=APILocator.getIdentifierAPI().find(legacyPage);
        VersionInfo vInfo=APILocator.getVersionableAPI().getVersionInfo(legacyident.getId());
        
        List<HTMLPageAsset> versions=new ArrayList<HTMLPageAsset>(); 
        
        HTMLPage working=(HTMLPage) APILocator.getVersionableAPI().findWorkingVersion(legacyident, user, respectFrontEndPermissions);
        HTMLPageAsset cworking=copyLegacyData(working, user, respectFrontEndPermissions), clive=null;
        if(vInfo.getLiveInode()!=null && !vInfo.getLiveInode().equals(vInfo.getWorkingInode())) {
            HTMLPage live=(HTMLPage) APILocator.getVersionableAPI().findLiveVersion(legacyident, user, respectFrontEndPermissions);
            clive=copyLegacyData(working, user, respectFrontEndPermissions);
        }
        
        List<Permission> perms=null;
        if(!APILocator.getPermissionAPI().isInheritingPermissions(legacyPage)) {
            perms = APILocator.getPermissionAPI().getPermissions(legacyPage, true, true, true);
        }
        
        List<MultiTree> multiTree = MultiTreeFactory.getMultiTree(working.getIdentifier());
        
        APILocator.getHTMLPageAPI().delete(working, user, respectFrontEndPermissions);
        PageServices.invalidate(working);
        HibernateUtil.getSession().clear();
        CacheLocator.getIdentifierCache().removeFromCacheByIdentifier(legacyident.getId());
        
        if(clive!=null) {
            Contentlet cclive = APILocator.getContentletAPI().checkin(clive, user, respectFrontEndPermissions);
            APILocator.getContentletAPI().publish(cclive, user, respectFrontEndPermissions);
        }
        
        Contentlet ccworking = APILocator.getContentletAPI().checkin(cworking, user, respectFrontEndPermissions);
        
        if(vInfo.getLiveInode()!=null && vInfo.getWorkingInode().equals(ccworking.getInode())) {
            APILocator.getContentletAPI().publish(ccworking, user, respectFrontEndPermissions);
        }
        
        for(MultiTree mt : multiTree) {
            MultiTreeFactory.saveMultiTree(mt);
        }
        
        APILocator.getPermissionAPI().removePermissions(ccworking);
        if(perms!=null) {
            APILocator.getPermissionAPI().permissionIndividually(ccworking.getParentPermissionable(), ccworking, user, respectFrontEndPermissions);
            APILocator.getPermissionAPI().assignPermissions(perms, ccworking, user, respectFrontEndPermissions);
        }
        
        return fromContentlet(ccworking);
    }
    
    @Override
    public String getHostDefaultPageType(String hostId) throws DotDataException, DotSecurityException {
        return getHostDefaultPageType(APILocator.getHostAPI().find(hostId, APILocator.getUserAPI().getSystemUser(), false));
    }
    
    @Override
    public String getHostDefaultPageType(Host host) {
        Field ff=host.getStructure().getField(DEFAULT_HTML_PAGE_ASSET_STRUCTURE_HOST_FIELD);
        if(ff!=null && InodeUtils.isSet(ff.getInode())) {
            String stInode= ff.getFieldType().equals(Field.FieldType.CONSTANT.toString()) ? ff.getValues()
                    : host.getStringProperty(ff.getVelocityVarName());
            if(stInode!=null && UtilMethods.isSet(stInode)) {
                Structure type=StructureCache.getStructureByInode(stInode);
                if(type!=null && InodeUtils.isSet(type.getInode())) {
                    return stInode;
                }
            }
        }
        return DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE;
    }

    @Override
    public boolean rename(HTMLPageAsset page, String newName, User user) throws DotDataException, DotSecurityException {
        Identifier sourceIdent=APILocator.getIdentifierAPI().find(page);
        Host host=APILocator.getHostAPI().find(sourceIdent.getHostId(), user, false);
        Identifier targetIdent=APILocator.getIdentifierAPI().find(host, 
                sourceIdent.getParentPath()+newName);
        if(targetIdent==null || !InodeUtils.isSet(targetIdent.getId())) {
            Contentlet cont=APILocator.getContentletAPI().checkout(page.getInode(), user, false);
            cont.setStringProperty(URL_FIELD, newName);
            cont=APILocator.getContentletAPI().checkin(cont, user, false);
            if(page.isLive()) {
                APILocator.getContentletAPI().publish(cont, user, false);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean move(HTMLPageAsset page, Folder parent, User user) throws DotDataException, DotSecurityException {
        return move(page,APILocator.getHostAPI().find(APILocator.getIdentifierAPI().find(parent).getHostId(),user,false), parent, user);
    }

    @Override
    public boolean move(HTMLPageAsset page, Host host, User user) throws DotDataException, DotSecurityException {
        return move(page,host,APILocator.getFolderAPI().findSystemFolder(),user);
    }
    
    public boolean move(HTMLPageAsset page, Host host, Folder parent, User user) throws DotDataException, DotSecurityException {
        Identifier sourceIdent=APILocator.getIdentifierAPI().find(page);
        Identifier targetFolderIdent=APILocator.getIdentifierAPI().find(parent);
        Identifier targetIdent=APILocator.getIdentifierAPI().find(host,targetFolderIdent.getURI()+sourceIdent.getAssetName());
        if(targetIdent==null || !InodeUtils.isSet(targetIdent.getId())) {
            Contentlet cont=APILocator.getContentletAPI().checkout(page.getInode(), user, false);
            cont.setFolder(parent.getInode());
            cont.setHost(host.getIdentifier());
            cont=APILocator.getContentletAPI().checkin(cont, user, false);
            if(page.isLive()) {
                APILocator.getContentletAPI().publish(cont, user, false);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Returns the ids for Pages whose Templates, Containers, or Content 
     * have been modified between 2 dates even if the page hasn't been modified
     * @param host Must be set
     * @param pattern url pattern e.g., /some/path/*
     * @param include the pattern is to include or exclude
     * @param startDate Must be set
     * @param endDate Must be Set
     * @return
     */
    @Override
    public List<String> findUpdatedHTMLPageIdsByURI(Host host, String pattern,boolean include,Date startDate, Date endDate) {

        Set<String> ret = new HashSet<String>();
        
        String likepattern=RegEX.replaceAll(pattern, "%", "\\*");
        
        String concat;
        if(DbConnectionFactory.isMySql()){
            concat=" concat(ii.parent_path, ii.asset_name) ";
        }else if (DbConnectionFactory.isMsSql()) {
            concat=" (ii.parent_path + ii.asset_name) ";
        }else {
            concat=" (ii.parent_path || ii.asset_name) ";
        }
        
        Structure st=StructureCache.getStructureByInode(DEFAULT_HTMLPAGE_ASSET_STRUCTURE_INODE);
        Field tf=st.getFieldVar(TEMPLATE_FIELD);
        
        // htmlpage with modified template
        StringBuilder bob = new StringBuilder();
        DotConnect dc = new DotConnect();
        bob.append("SELECT ii.id as pident ")
        .append("from identifier ii ")
        .append("join contentlet cc on (cc.identifier = ii.id) ")
        .append("join structure st on (cc.structure_inode=st.inode) ")
        .append("join template_version_info tvi on (cc.").append(tf.getFieldContentlet()).append(" = tvi.identifier) ")
        .append("where st.structuretype=").append(Structure.STRUCTURE_TYPE_HTMLPAGE)
        .append(" and tvi.version_ts >= ? and tvi.version_ts <= ? ")
        .append(" and ii.host_inode=? ")
        .append(" and ").append(concat).append(include?" LIKE ?":" NOT LIKE ?");
        dc.setSQL(bob.toString());
        dc.addParam(startDate);
        dc.addParam(endDate);
        dc.addParam(host.getIdentifier());
        dc.addParam(likepattern);
        try {
            for (Map<String,Object> row : dc.loadObjectResults())
                ret.add((String)row.get("pident"));
        } catch (DotDataException e) {
            Logger.error(this,"can't get pages asset with modified template. sql:"+bob,e);
        }
        
        // htmlpage with modified containers
        bob = new StringBuilder();
        bob.append("SELECT ii.id as pident ")
        .append("from identifier ii " )
        .append("join contentlet cc on (ii.id=cc.identifier) ")
        .append("join structure st on (cc.structure_inode=st.inode) ")
        .append("join template_containers tc on (cc.").append(tf.getFieldContentlet()).append(" = tc.template_id) ")
        .append("join container_version_info cvi on (tc.container_id = cvi.identifier) ")
        .append("where st.structuretype=").append(Structure.STRUCTURE_TYPE_HTMLPAGE)
        .append(" and cvi.version_ts >= ? and cvi.version_ts <= ? ")
        .append(" and ii.host_inode=? ")
        .append(" and ").append(concat).append(include?" LIKE ?":" NOT LIKE ?");
        dc.setSQL(bob.toString());
        dc.addParam(startDate);
        dc.addParam(endDate);
        dc.addParam(host.getIdentifier());
        dc.addParam(likepattern);
        try {
            for (Map<String,Object> row : dc.loadObjectResults())
                ret.add((String)row.get("pident"));
        } catch (DotDataException e) {
            Logger.error(this,"can't get modified containers under page asset sql:"+bob,e);
        }
        
        // htmlpages with modified content
        bob = new StringBuilder();
        bob.append("SELECT ii.id as pident ")
        .append("from contentlet_version_info hvi join identifier ii on (hvi.identifier=ii.id) " )
        .append("join contentlet cc on (ii.id=cc.identifier) ")
        .append("join structure st on (cc.structure_inode=st.inode) ")
        .append("join multi_tree mt on (hvi.identifier = mt.parent1) ")
        .append("join contentlet_version_info cvi on (mt.child = cvi.identifier) ")
        .append("where st.structuretype=").append(Structure.STRUCTURE_TYPE_HTMLPAGE)
        .append(" and cvi.version_ts >= ? and cvi.version_ts <= ? ")
        .append(" and ii.host_inode=? ")
        .append(" and ").append(concat).append(include?" LIKE ?":" NOT LIKE ?");
        dc.setSQL(bob.toString());
        dc.addParam(startDate);
        dc.addParam(endDate);
        dc.addParam(host.getIdentifier());
        dc.addParam(likepattern);
        
        try {
            for (Map<String,Object> row : dc.loadObjectResults())
                ret.add((String)row.get("pident"));
        } catch (DotDataException e) {
            Logger.error(this,"can't get mdified content under page asset sql:"+bob,e);
        }
        
        // htmlpage modified itself
        bob = new StringBuilder();
        bob.append("SELECT ii.id as pident from contentlet cc ")
        .append("join identifier ii on (ii.id=cc.identifier) ")
        .append("join contentlet_version_info vi on (vi.identifier=ii.id) ")
        .append("join structure st on (cc.structure_inode=st.inode) ")
        .append("where st.structuretype=").append(Structure.STRUCTURE_TYPE_HTMLPAGE)
        .append(" and vi.version_ts >= ? and vi.version_ts <= ? ")
        .append(" and ii.host_inode=? ")
        .append(" and ").append(concat).append(include?" LIKE ?":" NOT LIKE ?");
        dc.setSQL(bob.toString());
        dc.addParam(startDate);
        dc.addParam(endDate);
        dc.addParam(host.getIdentifier());
        dc.addParam(likepattern);
        
        try {
            for (Map<String,Object> row : dc.loadObjectResults())
                ret.add((String)row.get("pident"));
        } catch (DotDataException e) {
            Logger.error(this,"can't get modified page assets sql:"+bob,e);
        }
        
        return new ArrayList<String>(ret);
    }
    
    @Override
    public String getHTML(IHTMLPage htmlPage, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException {
		return getHTML(htmlPage, true, null, userAgent);
	}

    @Override
	public String getHTML(IHTMLPage htmlPage, boolean liveMode, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException {
		return getHTML(htmlPage, liveMode, null, userAgent);
	}

	@Override
	public String getHTML(IHTMLPage htmlPage, boolean liveMode,
			String contentId, String userAgent) throws DotStateException,
			DotDataException, DotSecurityException {
		return getHTML(htmlPage, liveMode, contentId, null, userAgent);
	}
	
	@Override
	public String getHTML(IHTMLPage htmlPage, boolean liveMode,
			String contentId, User user, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException {
		String uri = htmlPage.getURI();
		Host host = getParentHost(htmlPage);
		return getHTML(uri, host, liveMode, contentId, user, userAgent);
	}

	@Override
	public String getHTML(String uri, Host host, boolean liveMode,
			String contentId, User user, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException {
		return getHTML(uri, host, liveMode, contentId, user, 0, userAgent);
	}

	@Override
	public String getHTML(String uri, Host host, boolean liveMode,
			String contentId, User user, long langId, String userAgent)
			throws DotStateException, DotDataException, DotSecurityException {
		/*
		 * The below code is copied from VelocityServlet.doLiveMode() and
		 * modified to parse a HTMLPage. Replaced the request and response
		 * objects with DotRequestProxy and DotResponseProxyObjects.
		 * 
		 * TODO Code clean-up.
		 * 
		 * TODO: I don't think it will work - jorge.urdaneta
		 */

		InvocationHandler dotInvocationHandler = new DotInvocationHandler(
				new HashMap());

		DotRequestProxy requestProxy = (DotRequestProxy) Proxy
				.newProxyInstance(DotRequestProxy.class.getClassLoader(),
						new Class[] { DotRequestProxy.class },
						dotInvocationHandler);

		DotResponseProxy responseProxy = (DotResponseProxy) Proxy
				.newProxyInstance(DotResponseProxy.class.getClassLoader(),
						new Class[] { DotResponseProxy.class },
						dotInvocationHandler);

		StringWriter out = new StringWriter();
		Context context = null;



		// Map with all identifier inodes for a given uri.
		String idInode = APILocator.getIdentifierAPI().find(host, uri)
				.getInode();

		String cachedUri;
		if(UtilMethods.isSet(langId)){
			// Checking the path is really live using the livecache
			cachedUri = (liveMode) ? LiveCache.getPathFromCache(uri, host, langId)
					: WorkingCache.getPathFromCache(uri, host, langId);
		}else{
			// Checking the path is really live using the livecache
						cachedUri = (liveMode) ? LiveCache.getPathFromCache(uri, host)
								: WorkingCache.getPathFromCache(uri, host);
		}

		// if we still have nothing.
		if (!InodeUtils.isSet(idInode) || cachedUri == null) {
			throw new ResourceNotFoundException(String.format(
					"Resource %s not found in Live mode!", uri));
		}

		responseProxy.setContentType("text/html");
		requestProxy.setAttribute("User-Agent", userAgent);
		requestProxy.setAttribute("idInode", String.valueOf(idInode));

		/* Set long lived cookie regardless of who this is */
		String _dotCMSID = UtilMethods.getCookieValue(
				requestProxy.getCookies(),
				com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

		if (!UtilMethods.isSet(_dotCMSID)) {
			/* create unique generator engine */
			Cookie idCookie = CookieUtil.createCookie();
			responseProxy.addCookie(idCookie);
		}

		requestProxy.put("host", host);
		requestProxy.put("host_id", host.getIdentifier());
		requestProxy.put("uri", uri);
		requestProxy.put("user", user);
		if (!liveMode) {
			requestProxy.setAttribute(WebKeys.PREVIEW_MODE_SESSION, "true");
		}
		boolean signedIn = false;

		if (user != null) {
			signedIn = true;
		}
		Identifier ident = APILocator.getIdentifierAPI().find(host, uri);

		Logger.debug(HTMLPageAssetAPIImpl.class, "Page Permissions for URI=" + uri);

		IHTMLPage pageProxy = new HTMLPageAsset();
		pageProxy.setIdentifier(ident.getInode());

		// Check if the page is visible by a CMS Anonymous role
		try {
			if (!permissionAPI.doesUserHavePermission(pageProxy,
					PermissionAPI.PERMISSION_READ, user, true)) {
				// this page is protected. not anonymous access

				/*******************************************************************
				 * If we need to redirect someone somewhere to login before
				 * seeing a page, we need to edit the /portal/401.jsp page to
				 * sendRedirect the user to the proper login page. We are not
				 * using the REDIRECT_TO_LOGIN variable in the config any
				 * longer.
				 ******************************************************************/
				if (!signedIn) {
					// No need for the below LAST_PATH attribute on the front
					// end http://jira.dotmarketing.net/browse/DOTCMS-2675
					// request.getSession().setAttribute(WebKeys.LAST_PATH,
					// new ObjectValuePair(uri, request.getParameterMap()));
					requestProxy.getSession().setAttribute(
							com.dotmarketing.util.WebKeys.REDIRECT_AFTER_LOGIN,
							uri);

					Logger.debug(HTMLPageAssetAPIImpl.class,
							"VELOCITY CHECKING PERMISSION: Page doesn't have anonymous access"
									+ uri);

					Logger.debug(HTMLPageAssetAPIImpl.class, "401 URI = " + uri);

					Logger.debug(HTMLPageAssetAPIImpl.class, "Unauthorized URI = "
							+ uri);
					responseProxy.sendError(401,
							"The requested page/file is unauthorized");
					return "An SYSTEM ERROR OCCURED !";

				} else if (!permissionAPI.getReadRoles(ident).contains(
						APILocator.getRoleAPI().loadLoggedinSiteRole())) {
					// user is logged in need to check user permissions
					Logger.debug(HTMLPageAssetAPIImpl.class,
							"VELOCITY CHECKING PERMISSION: User signed in");

					// check user permissions on this asset
					if (!permissionAPI.doesUserHavePermission(ident,
							PermissionAPI.PERMISSION_READ, user, true)) {
						// the user doesn't have permissions to see this page
						// go to unauthorized page
						Logger.warn(HTMLPageAssetAPIImpl.class,
								"VELOCITY CHECKING PERMISSION: Page doesn't have any access for this user");
						responseProxy.sendError(403,
								"The requested page/file is forbidden");
						return "PAGE NOT FOUND!";
					}
				}
			}

			if (UtilMethods.isSet(contentId)) {
				requestProxy.setAttribute(WebKeys.WIKI_CONTENTLET, contentId);
			}

			if (langId > 0) {
				requestProxy.setAttribute(WebKeys.HTMLPAGE_LANGUAGE,
						Long.toString(langId));
			}
			LanguageWebAPI langWebAPI = WebAPILocator.getLanguageWebAPI();
			langWebAPI.checkSessionLocale(requestProxy);

			context = VelocityUtil.getWebContext(requestProxy, responseProxy);

			if (langId > 0) {
				context.put("language", Long.toString(langId));
			}

			if (!liveMode) {
				context.put("PREVIEW_MODE", new Boolean(true));
			} else {
				context.put("PREVIEW_MODE", new Boolean(false));
			}

			context.put("host", host);
			VelocityEngine ve = VelocityUtil.getEngine();

			Logger.debug(HTMLPageAssetAPIImpl.class, "Got the template!!!!"
					+ idInode);

			requestProxy.setAttribute("velocityContext", context);
			
			String langStr = "_" + Long.toString(APILocator.getLanguageAPI().getDefaultLanguage().getId());
			
			if(UtilMethods.isSet(contentId)) {
				langStr = "_" + APILocator.getContentletAPI().find(contentId, user, false).getLanguageId();
			}

			String VELOCITY_HTMLPAGE_EXTENSION = Config
					.getStringProperty("VELOCITY_HTMLPAGE_EXTENSION");
			String vTempalate = (liveMode) ? "/live/" + idInode + langStr + "."
					+ VELOCITY_HTMLPAGE_EXTENSION : "/working/" + idInode + langStr + "."
					+ VELOCITY_HTMLPAGE_EXTENSION;

			ve.getTemplate(vTempalate).merge(context, out);

		} catch (Exception e1) {
			Logger.error(this, e1.getMessage(), e1);
		} finally {
			context = null;
			VelocityServlet.velocityCtx.remove();
		}

		if (Config.getBooleanProperty("ENABLE_CLICKSTREAM_TRACKING", false)) {
			Logger.debug(HTMLPageAssetAPIImpl.class, "Into the ClickstreamFilter");
			// Ensure that clickstream is recorded only once per request.
			if (requestProxy.getAttribute(ClickstreamFilter.FILTER_APPLIED) == null) {
				requestProxy.setAttribute(ClickstreamFilter.FILTER_APPLIED,
						Boolean.TRUE);

				if (user != null) {
					UserProxy userProxy = null;
					try {
						userProxy = com.dotmarketing.business.APILocator
								.getUserProxyAPI()
								.getUserProxy(
										user,
										APILocator.getUserAPI().getSystemUser(),
										false);
					} catch (DotRuntimeException e) {
						e.printStackTrace();
					} catch (DotSecurityException e) {
						e.printStackTrace();
					} catch (DotDataException e) {
						e.printStackTrace();
					}

				}
			}
		}

		return out.toString();
	}
}
