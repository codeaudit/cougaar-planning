# NonCSMARTDBAssetInitializer.q
# Used by planning's Asset Initialization code
# to read asset definitions from the CSMART03 reference
# database.
# See org.cougaar.planning.ldm.asset.NonCSMARTDBInitializerServiceImpl
database=${org.cougaar.refconfig.database}
username=${org.cougaar.refconfig.user}
password=${org.cougaar.refconfig.password}

queryAgentPrototype = \
 SELECT ORG_CLASS \
   FROM lib_organization \
  WHERE \
    ORG_NAME = ':agent_name:'

queryAgentPGNames = \
 SELECT distinct A.PG_NAME \
   FROM lib_organization H, \
        org_pg_attr B, \
        lib_pg_attribute A \
  WHERE H.ORG_ID = B.ORG_ID \
    AND A.PG_ATTRIBUTE_LIB_ID = B.PG_ATTRIBUTE_LIB_ID \
    AND H.ORG_NAME = ':agent_name:'

queryLibProperties = \
 SELECT ATTRIBUTE_NAME, ATTRIBUTE_TYPE, AGGREGATE_TYPE, \
        PG_ATTRIBUTE_LIB_ID \
   FROM lib_pg_attribute \
  WHERE PG_NAME = ':pg_name:'

queryAgentProperties = \
 SELECT A.ATTRIBUTE_VALUE \
   FROM org_pg_attr A, lib_organization B \
  WHERE \
    A.ORG_ID = B.ORG_ID \
    AND B.ORG_NAME = ':agent_name:' \
    AND A.PG_ATTRIBUTE_LIB_ID = ':pg_attribute_id:'

queryAgentRelation = \
 SELECT ASB_REL.ROLE, SPTD.ORG_NAME ITEM_IDENTIFICATION, \
        ASB_PG.ATTRIBUTE_VALUE TYPE_IDENTIFICATION, SPTD.ORG_NAME SUPPORTED, \
        date_format(ASB_REL.start_date, '%m/%e/%Y %l:%i %p'), \
        date_format(ASB_REL.end_date, '%m/%e/%Y %l:%i %p') \
   FROM org_relation ASB_REL, \
        lib_pg_attribute LIB_PG, \
        org_pg_attr ASB_PG, \
        lib_organization SPTD, \
        lib_organization SPTG \
  WHERE LIB_PG.PG_ATTRIBUTE_LIB_ID = ASB_PG.PG_ATTRIBUTE_LIB_ID \
    AND ASB_PG.ORG_ID = ASB_REL.SUPPORTED_ORG_ID \
    AND ASB_REL.SUPPORTING_ORG_ID = SPTG.ORG_ID \
    AND ASB_REL.SUPPORTED_ORG_ID = SPTD.ORG_ID \
    AND LIB_PG.PG_NAME = 'TypeIdentificationPG' \
    AND LIB_PG.ATTRIBUTE_NAME = 'TypeIdentification' \
    AND SPTG.ORG_NAME = ':agent_name:'

# This query used for MilitaryOrgPG.HomeLocation when creating
# OrgAssets when running from the DB. This happens
# because in the lib_pg table in the DB this query name
# is listed.
# Sometimes units are listed as at 'fake' GEOLOCs, ALPLOCs
# So union with the ALPLOC table
queryGeolocLocation = \
 SELECT 'GeolocLocation', ':key:' \
      ||', InstallationTypeCode=' || INSTALLATION_TYPE_CODE \
      ||', CountryStateCode='     || COUNTRY_STATE_CODE \
      ||', CountryStateName='     || REPLACE(COUNTRY_STATE_LONG_NAME, ' ', '_') \
      ||', Name='                 || REPLACE(LOCATION_NAME, ' ', '_') \
      ||', Latitude=Latitude '    || LATITUDE || 'degrees' \
      ||', Longitude=Longitude '  || LONGITUDE || 'degrees' \
   FROM geoloc \
  WHERE GEOLOC_CODE = SUBSTR(':key:', 12) 

## Except the default org.cougaar.database does not have
# an ALPLOC table. Now, if you have installed everything
# from our one dump, you get the data. But for now
# you dont, so we dont do the union, and you better
# have a real GEOLOC
#      UNION \
# SELECT 'GeolocLocation', ':key:' \
#      ||', InstallationTypeCode=' \
#      ||', CountryStateCode='     \
#      ||', CountryStateName='     \
#      ||', Name='                 || REPLACE(LOCATION_NAME, ' ', '_') \
#      ||', Latitude=Latitude '    || LATITUDE || 'degrees' \
#      ||', Longitude=Longitude '  || LONGITUDE || 'degrees' \
#   FROM alploc \
#  WHERE ALPLOC_CODE = SUBSTR(':key:', 12)

# FIXME: This needs to be unioned with the ALPLOC table, like above, 
# but MySQL won't support that until v4 is out of alpha
queryGeolocLocation.mysql = \
 SELECT 'GeolocLocation', concat(':key:' \
      ,', InstallationTypeCode=' , INSTALLATION_TYPE_CODE \
      ,', CountryStateCode='     , COUNTRY_STATE_CODE \
      ,', CountryStateName='     , REPLACE(COUNTRY_STATE_LONG_NAME, ' ', '_') \
      ,', Name='                 , REPLACE(LOCATION_NAME, ' ', '_') \
      ,', Latitude=Latitude '    , LATITUDE , 'degrees' \
      ,', Longitude=Longitude '  , LONGITUDE , 'degrees') \
   FROM geoloc \
  WHERE GEOLOC_CODE = SUBSTRING(':key:', 12)
