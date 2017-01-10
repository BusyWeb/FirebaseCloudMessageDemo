
CREATE TABLE [dbo].[FCMDemoUsers] (
    [Id]          INT             NOT NULL Identity(1000, 1),
    [Email]       NVARCHAR (256)  NOT NULL,
    [UserId]      NVARCHAR (1024) NOT NULL,
    [DeviceToken] NVARCHAR (2048) NOT NULL,
    PRIMARY KEY CLUSTERED ([Id] ASC)
);