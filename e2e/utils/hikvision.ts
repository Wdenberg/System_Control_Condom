import { expect, type APIRequestContext } from '@playwright/test';

export function buildAnprXml({ plate, uuid, ip = '192.168.15.43', occurredAt = new Date().toISOString() }: { plate: string; uuid: string; ip?: string; occurredAt?: string }) {
  return `<?xml version="1.0" encoding="UTF-8"?>
<EventNotificationAlert version="2.0" xmlns="http://www.hikvision.com/ver20/XMLSchema">
<ipAddress>${ip}</ipAddress>
<ipv6Address>::ffff:${ip}</ipv6Address>
<portNo>80</portNo>
<protocol>HTTP</protocol>
<macAddress>40:ac:bf:9d:36:8d</macAddress>
<channelID>1</channelID>
<dateTime>${occurredAt}</dateTime>
<activePostCount>1</activePostCount>
<eventType>ANPR</eventType>
<eventState>active</eventState>
<eventDescription>ANPR</eventDescription>
<channelName>Camera 01</channelName>
<ANPR>
<country>220</country>
<licensePlate>${plate}</licensePlate>
<line>1</line>
<direction>forward</direction>
<confidenceLevel>95</confidenceLevel>
<plateType>unknown</plateType>
<plateColor>unknown</plateColor>
<licenseBright>130</licenseBright>
<dangmark>no</dangmark>
<twoWheelVehicle>no</twoWheelVehicle>
<threeWheelVehicle>no</threeWheelVehicle>
<plateCharBelieve>99,99,99,99,99,99,99</plateCharBelieve>
<vehicleType>SUVMPV</vehicleType>
<detectDir>8</detectDir>
<detectType>0</detectType>
<alarmDataType>0</alarmDataType>
<vehicleInfo>
<index>2018</index>
<colorDepth>2</colorDepth>
<color>white</color>
<length>0</length>
<vehicleLogoRecog>1123</vehicleLogoRecog>
<vehileSubLogoRecog>0</vehileSubLogoRecog>
<vehileModel>0</vehileModel>
</vehicleInfo>
<pictureInfoList>
<pictureInfo>
<fileName>licensePlatePicture.jpg</fileName>
<type>licensePlatePicture</type>
<dataType>0</dataType>
<picRecogMode>0</picRecogMode>
<absTime>20260913140000000</absTime>
<pId>${uuid}-plate</pId>
</pictureInfo>
<pictureInfo>
<fileName>vehiclePicture.jpg</fileName>
<type>vehiclePicture</type>
<dataType>0</dataType>
<picRecogMode>0</picRecogMode>
<absTime>20260913140000000</absTime>
<pId>${uuid}-vehicle</pId>
</pictureInfo>
<pictureInfo>
<fileName>detectionPicture.jpg</fileName>
<type>detectionPicture</type>
<dataType>0</dataType>
<picRecogMode>0</picRecogMode>
<absTime>20260913140000000</absTime>
<plateRect><X>512</X><Y>287</Y><width>46</width><height>19</height></plateRect>
<pId>${uuid}-detection</pId>
</pictureInfo>
</pictureInfoList>
<originalLicensePlate>${plate}</originalLicensePlate>
<CRIndex>220</CRIndex>
<vehicleListName>blockList</vehicleListName>
<plateSize>0</plateSize>
</ANPR>
<UUID>${uuid}</UUID>
<picNum>3</picNum>
<monitoringSiteID></monitoringSiteID>
<isDataRetransmission>false</isDataRetransmission>
</EventNotificationAlert>`;
}

export async function sendHikvisionEvent(
  request: APIRequestContext,
  plate: string,
  uuid: string,
  webhookToken: string,
  ip = '192.168.15.43',
  occurredAt?: string,
) {
  const response = await request.post('/api/v1/integrations/hikvision/events', {
    headers: {
      'Content-Type': 'application/xml',
      'X-Hikvision-Webhook-Token': webhookToken,
    },
    data: buildAnprXml({ plate, uuid, ip, occurredAt }),
  });
  await expect(response).toBeOK();
  const body: unknown = await response.json();
  if (!body || typeof body !== 'object'
    || !('eventId' in body) || typeof body.eventId !== 'number'
    || !('status' in body) || typeof body.status !== 'string'
    || !('plate' in body) || typeof body.plate !== 'string'
    || !('movementId' in body) || !(body.movementId === null || typeof body.movementId === 'number')
    || !('movementStatus' in body) || !(body.movementStatus === null || typeof body.movementStatus === 'string')
    || !('accessDecision' in body) || !(body.accessDecision === null || typeof body.accessDecision === 'string')) throw new Error('Invalid Hikvision response contract');
  return { eventId: body.eventId, status: body.status, plate: body.plate, movementId: body.movementId, movementStatus: body.movementStatus, accessDecision: body.accessDecision };
}
