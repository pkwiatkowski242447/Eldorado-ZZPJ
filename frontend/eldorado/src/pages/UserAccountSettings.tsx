import {SetStateAction, useEffect, useState} from 'react';
import SiteHeader from "@/components/SiteHeader.tsx";
import {z} from "zod";
import {zodResolver} from "@hookform/resolvers/zod";
import {Form, FormControl, FormField, FormItem, FormLabel, FormMessage} from "@/components/ui/form.tsx";
import {Button} from "@/components/ui/button.tsx";
import {Controller, useForm} from "react-hook-form";
import {Card, CardContent} from "@/components/ui/card.tsx";
import {Input} from "@/components/ui/input.tsx";
import {isValidPhoneNumber} from "react-phone-number-input/min";
import {PhoneInput} from "@/components/ui/phone-input.tsx";
import {api} from "@/api/api.ts";
import {toast} from "@/components/ui/use-toast.ts";
import {useAccount} from "@/hooks/useAccount.ts";
import {useParams} from "react-router-dom";
import {UserType} from "@/types/Users.ts";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogTitle
} from "@/components/ui/alert-dialog.tsx";
import {
    Breadcrumb,
    BreadcrumbItem,
    BreadcrumbLink,
    BreadcrumbList,
    BreadcrumbSeparator
} from "@/components/ui/breadcrumb.tsx";
import {RolesEnum} from "@/types/TokenPayload.ts";
import {useTranslation} from "react-i18next";
import handleApiError from "@/components/HandleApiError.ts";
import {Loader2, Slash} from "lucide-react";
import {Switch} from "@/components/ui/switch.tsx";
import {FiCheck, FiX} from "react-icons/fi";

const allUserLevels: RolesEnum[] = [RolesEnum.ADMIN, RolesEnum.STAFF, RolesEnum.CLIENT];

function UserAccountSettings() {
    const [activeForm, setActiveForm] = useState('UserLevels');
    const [managedUser, setManagedUser] = useState<UserType | null>(null);
    const [isAlertDialogOpen, setAlertDialogOpen] = useState(false);
    const [levelToChange, setLevelToChange] = useState<RolesEnum | null>(null);
    const {id} = useParams<{ id: string }>();
    const {t} = useTranslation();
    const [formValues, setFormValues] = useState(null);
    const [formType, setFormType] = useState(null);
    const [isLoading, setIsLoading] = useState(false);
    const [isRefreshing, setIsRefreshing] = useState(false);

    const emailSchema = z.object({
        email: z.string().email({message: t("accountSettings.wrongEmail")}),
    });

    const userDataSchema = z.object({
        name: z.string().min(2, {message: t("accountSettings.firstNameTooShort")})
            .max(50, {message: t("accountSettings.firstNameTooLong")}),
        lastName: z.string().min(2, {message: t("accountSettings.lastNameTooShort")})
            .max(50, {message: t("accountSettings.lastNameTooLong")}),
        phoneNumber: z.string().refine(isValidPhoneNumber, {message: t("accountSettings.phoneNumberInvalid")}),
        twoFactorAuth: z.boolean().optional().default(managedUser?.twoFactorAuth || false),
    });

    useEffect(() => {
        if (id) {
            api.getAccountById(id).then(response => {
                setManagedUser(response.data);
                window.localStorage.setItem('etag', response.headers['etag']);
            });
        }
    }, [id]);

    const {getCurrentAccount} = useAccount();

    const handleDialogAction = () => {
        if (formType === 'email') {
            // @ts-expect-error - fix this
            SubmitEmail(formValues);
        } else if (formType === 'userData') {
            // @ts-expect-error - fix this
            SubmitUserData(formValues);
        } else {
            SubmitPassword();
        }
        setAlertDialogOpen(false);
    };

    const handleRemoveClick = (level: RolesEnum) => {
        setLevelToChange(level);
        setAlertDialogOpen(true);
    };

    const handleAddClick = (level: RolesEnum) => {
        setLevelToChange(level);
        setAlertDialogOpen(true);
    };

    const onSubmitEmail = (values: SetStateAction<null>) => {
        setFormValues(values);
        // @ts-expect-error - fix this
        setFormType('email');
        setAlertDialogOpen(true);
    };

    const onSubmitUserData = (values: SetStateAction<null>) => {
        setFormValues(values);
        //@ts-expect-error - fix this
        setFormType('userData');
        setAlertDialogOpen(true);
    };

    const onSubmitPassword = () => {
        setAlertDialogOpen(true);
    };

    //TODO you have to manually refresh the page to see the changes
    //TODO gray out the admin button for the admin that is editing his own account
    const confirmChangeUserLevel = () => {
        if (levelToChange && managedUser) {
            if (managedUser?.userLevelsDto.some(userLevel => userLevel.roleName === levelToChange)) {
                switch (levelToChange) {
                    case RolesEnum.ADMIN:
                        api.removeLevelAdmin(managedUser.id).then(() => {
                            setAlertDialogOpen(false);
                            toast({
                                title: t("accountSettings.popUp.changeUserDataOK.title"),
                                description: t("accountSettings.popUp.changeUserDataOK.userLevelRemoved")
                            })
                        }).catch((error) => {
                            handleApiError(error);
                        });
                        break;
                    case RolesEnum.STAFF:
                        api.removeLevelStaff(managedUser.id).then(() => {
                            setAlertDialogOpen(false);
                            toast({
                                title: t("accountSettings.popUp.changeUserDataOK.title"),
                                description: t("accountSettings.popUp.changeUserDataOK.userLevelRemoved")
                            })
                        }).catch((error) => {
                            handleApiError(error);
                        });
                        break;
                    case RolesEnum.CLIENT:
                        api.removeLevelClient(managedUser.id).then(() => {
                            setAlertDialogOpen(false);
                            toast({
                                title: t("accountSettings.popUp.changeUserDataOK.title"),
                                description: t("accountSettings.popUp.changeUserDataOK.userLevelRemoved")
                            })
                        }).catch((error) => {
                            handleApiError(error);
                        });
                        break;
                }
            } else {
                switch (levelToChange) {
                    case RolesEnum.ADMIN:
                        api.addLevelAdmin(managedUser.id).then(() => {
                            setAlertDialogOpen(false);
                            toast({
                                title: t("accountSettings.popUp.changeUserDataOK.title"),
                                description: t("accountSettings.popUp.changeUserDataOK.userLevelAdded")
                            })
                        }).catch((error) => {
                            handleApiError(error);
                        });
                        break;
                    case RolesEnum.STAFF:
                        api.addLevelStaff(managedUser.id).then(() => {
                            setAlertDialogOpen(false);
                            toast({
                                title: t("accountSettings.popUp.changeUserDataOK.title"),
                                description: t("accountSettings.popUp.changeUserDataOK.userLevelAdded")
                            })
                        }).catch((error) => {
                            handleApiError(error);
                        });
                        break;
                    case RolesEnum.CLIENT:
                        api.addLevelClient(managedUser.id).then(() => {
                            setAlertDialogOpen(false);
                            toast({
                                title: t("accountSettings.popUp.changeUserDataOK.title"),
                                description: t("accountSettings.popUp.changeUserDataOK.userLevelAdded")
                            })
                        }).catch((error) => {
                            handleApiError(error);
                        });
                        break;
                }
                setAlertDialogOpen(false);
                setLevelToChange(null);
            }
        }
    }

    const formEmail = useForm({
        resolver: zodResolver(emailSchema),
    });

    const formUserData = useForm({
        resolver: zodResolver(userDataSchema),
    });

    const SubmitEmail = (values: z.infer<typeof emailSchema>) => {
        if (managedUser) {
            setIsLoading(true)
            api.changeEmailUser(managedUser.id, values.email).then(() => {
                toast({
                    title: t("accountSettings.popUp.changeUserDataOK.title"),
                    description: t("accountSettings.popUp.changeEmailOK.text"),
                });
                if (managedUser?.id) {
                    api.getAccountById(managedUser?.id).then(response => {
                        setManagedUser(response.data);
                    });
                }
                setFormType(null);
                setFormValues(null);

            }).catch((error) => {
                handleApiError(error);
            }).finally(() => {
                setIsLoading(false);
            });
        }
    };

    const SubmitUserData = (values: z.infer<typeof userDataSchema>) => {
        setIsLoading(true)
        const etag = window.localStorage.getItem('etag');
        if (managedUser && managedUser.accountLanguage && etag !== null) {
            if (values.twoFactorAuth === undefined) {
                values.twoFactorAuth = false;
            }
            api.modifyAccountUser(managedUser.login, managedUser.version, managedUser.userLevelsDto,
                values.name, values.lastName, values.phoneNumber, values.twoFactorAuth, etag)
                .then(() => {
                    getCurrentAccount();
                    toast({
                        title: t("accountSettings.popUp.changeUserDataOK.title"),
                        description: t("accountSettings.popUp.changeOtherUserDataOK.text"),
                    });
                    setFormType(null);
                    setFormValues(null);

                }).catch((error) => {
                handleApiError(error);
            }).finally(() => {
                setIsLoading(false);
            });
        } else {
            console.log('Account or account language is not defined');
        }
    };

    const SubmitPassword = () => {
        if (id) {
            setIsLoading(true)
            api.resetPasswordByAdmin(id).then(() => {
                getCurrentAccount();
                toast({
                    title: t("accountSettings.popUp.changePasswordOK.title"),
                    description: t("accountSettings.popUp.changePasswordOK.text"),
                });
                setFormType(null);
                setFormValues(null);

            }).catch((error) => {
                handleApiError(error);
            }).finally(() => {
                setIsLoading(false);
            });
        }

    };

    const refresh = () => {
        setIsRefreshing(true);
        if (id) {
            api.getAccountById(id).then(response => {
                setManagedUser(response.data);
                window.localStorage.setItem('etag', response.headers['etag']);
            });
        }
        setTimeout(() => setIsRefreshing(false), 1000);
    }

    return (
        <div>
            <SiteHeader/>
            <div className="flex justify-between items-center pt-2">
            <Breadcrumb className={"pl-2"}>
                <BreadcrumbList>
                    <BreadcrumbItem>
                        <BreadcrumbLink href="/home">{t("breadcrumb.home")}</BreadcrumbLink>
                    </BreadcrumbItem>
                    <BreadcrumbSeparator>
                        <Slash/>
                    </BreadcrumbSeparator>
                    <BreadcrumbItem>
                        <BreadcrumbLink href="/manage-users">{t("breadcrumb.manageUsers")}</BreadcrumbLink>
                    </BreadcrumbItem>
                    <BreadcrumbSeparator>
                        <Slash/>
                    </BreadcrumbSeparator>
                    <BreadcrumbItem>
                        <BreadcrumbLink>{t("breadcrumb.userAccount")}</BreadcrumbLink>
                    </BreadcrumbItem>
                </BreadcrumbList>
            </Breadcrumb>
                <Button onClick={refresh} variant={"ghost"} className="w-auto" disabled={isRefreshing}>
                    {isRefreshing ? (
                        <>
                            <Loader2 className="mr-2 h-4 w-4 animate-spin"/>
                        </>
                    ) : (
                        t("general.refresh")
                    )}
                </Button>
            </div>
            <main
                className="flex min-h-[calc(100vh_-_theme(spacing.16))] flex-1 flex-col gap-4 bg-muted/40 p-4 md:gap-8 md:p-10">
                <div className="mx-auto grid w-full max-w-6xl gap-2">
                    <h1 className="text-3xl font-semibold">{t("accountSettings.users.table.settings.account.title")}{managedUser?.login}</h1>
                </div>
                <div
                    className="mx-auto grid w-full max-w-6xl items-start gap-6 md:grid-cols-[180px_1fr] lg:grid-cols-[250px_1fr]">
                    <nav
                        className="grid gap-4 text-sm text-muted-foreground"
                    >
                        <Button variant={`${activeForm === 'UserLevels' ? 'outline' : 'ghost'}`}
                                onClick={() => setActiveForm('UserLevels')}
                                className={`text-muted-foreground transition-colors hover:text-foreground`}>
                            {t("accountSettings.users.table.settings.account.userLevels")}
                        </Button>
                        <Button variant={`${activeForm === 'E-Mail' ? 'outline' : 'ghost'}`}
                                onClick={() => setActiveForm('E-Mail')}
                                className={`text-muted-foreground transition-colors hover:text-foreground`}>
                            {t("accountSettings.email")}
                        </Button>
                        <Button variant={`${activeForm === 'Password' ? 'outline' : 'ghost'}`}
                                onClick={() => setActiveForm('Password')}
                                className={`text-muted-foreground transition-colors hover:text-foreground`}>
                            {t("accountSettings.password")}
                        </Button>
                        <Button variant={`${activeForm === 'Personal Info' ? 'outline' : 'ghost'}`}
                                onClick={() => setActiveForm('Personal Info')}
                                className={`text-muted-foreground transition-colors hover:text-foreground ${activeForm === 'Personal Info' ? 'font-bold' : ''}`}>
                            {t("accountSettings.users.table.settings.account.personalInfo")}
                        </Button>
                        <Button variant={`${activeForm === 'Details' ? 'outline' : 'ghost'}`}
                                onClick={() => setActiveForm('Details')}
                                className={`text-muted-foreground transition-colors hover:text-foreground ${activeForm === 'Details' ? 'font-bold' : ''}`}>
                            {t("accountSettings.details")}
                        </Button>
                    </nav>
                    <div className="grid gap-6">
                        {activeForm === 'UserLevels' && managedUser?.userLevelsDto && (
                            <div>
                                {activeForm === 'UserLevels' && (
                                    <div>
                                        {allUserLevels.map((level: RolesEnum) => (
                                            <div key={level}>
                                                <span>{level}</span>
                                                {managedUser?.userLevelsDto.some(userLevel => userLevel.roleName === level) ? (
                                                    <Button className="text-white bg-red-500 hover:bg-red-700 m-5"
                                                            onClick={() => handleRemoveClick(level)}>
                                                        {t("accountSettings.users.table.settings.account.userLevels.remove")}
                                                    </Button>
                                                ) : (
                                                    <Button
                                                        className="text-white bg-green-500 hover:bg-green-700 m-5"
                                                        onClick={() => handleAddClick(level)}>
                                                        {t("accountSettings.users.table.settings.account.userLevels.add")}
                                                    </Button>
                                                )}
                                            </div>
                                        ))}
                                        <AlertDialog open={isAlertDialogOpen} onOpenChange={setAlertDialogOpen}>
                                            <AlertDialogContent>
                                                <AlertDialogTitle>{t("general.confirm")}</AlertDialogTitle>
                                                <AlertDialogDescription>
                                                    {t("accountSettings.users.table.settings.block.confirm1")}
                                                    {managedUser?.userLevelsDto.some(userLevel => userLevel.roleName === levelToChange)
                                                        ? t("accountSettings.users.table.settings.account.userLevels.remove2") :
                                                        t("accountSettings.users.table.settings.account.userLevels.add2")} {levelToChange}
                                                    {t("accountSettings.users.table.settings.block.level")}
                                                    {managedUser?.userLevelsDto.some(userLevel => userLevel.roleName === levelToChange)
                                                        ? t("accountSettings.users.table.settings.block.from") : t("accountSettings.users.table.settings.block.to")}
                                                    {t("accountSettings.users.table.settings.block.confirm2")}
                                                </AlertDialogDescription>
                                                <AlertDialogAction
                                                    onClick={confirmChangeUserLevel}>{t("general.ok")}</AlertDialogAction>
                                                <AlertDialogCancel>{t("general.cancel")}</AlertDialogCancel>
                                            </AlertDialogContent>
                                        </AlertDialog>
                                    </div>
                                )}
                            </div>
                        )}
                        {activeForm === 'E-Mail' && (
                            <div>
                                <Card className="mx-10 w-auto">
                                    <CardContent>
                                        <Form {...formEmail}>
                                            {// @ts-expect-error - fix this maybe
                                                <form onSubmit={formEmail.handleSubmit(onSubmitEmail)}
                                                      className="space-y-4">
                                                    <div className="grid gap-4 p-5">
                                                        <div className="grid gap-2">
                                                            <FormField
                                                                control={formEmail.control}
                                                                name="email"
                                                                render={({field}) => (
                                                                    <FormItem>
                                                                        <FormLabel
                                                                            className="text-black">{t("accountSettings.users.table.settings.account.authentication.email")} *</FormLabel>
                                                                        <FormControl>
                                                                            <Input
                                                                                placeholder={managedUser?.email} {...field} />
                                                                        </FormControl>
                                                                        <FormMessage/>
                                                                    </FormItem>
                                                                )}/>
                                                        </div>
                                                        <Button type="submit" className="w-full pb-2"
                                                                disabled={isLoading}>
                                                            {isLoading ? (
                                                                <>
                                                                    <Loader2 className="mr-2 h-4 w-4 animate-spin"/>
                                                                </>
                                                            ) : (
                                                                t("accountSettings.users.table.settings.account.authentication.email.change")
                                                            )}
                                                        </Button>
                                                    </div>
                                                </form>
                                            }
                                        </Form>
                                    </CardContent>
                                </Card>
                                <AlertDialog open={isAlertDialogOpen} onOpenChange={setAlertDialogOpen}>
                                    <AlertDialogContent>
                                        <AlertDialogTitle>{t("general.confirm")}</AlertDialogTitle>
                                        <AlertDialogDescription>
                                            {t("accountSettings.confirmEmailChange")}
                                        </AlertDialogDescription>
                                        <AlertDialogAction onClick={handleDialogAction}>
                                            {t("general.ok")}
                                        </AlertDialogAction>
                                        <AlertDialogCancel>{t("general.cancel")}</AlertDialogCancel>
                                    </AlertDialogContent>
                                </AlertDialog>
                            </div>
                        )}
                        {activeForm === 'Password' && (
                            <div>
                                <Card className="mx-10 w-auto">
                                    <CardContent className={"flex items-center justify-center pt-5"}>
                                        <Button onClick={onSubmitPassword} className="w-full pb-2">
                                            {t("resetPasswordPage.title")}
                                        </Button>
                                    </CardContent>
                                </Card>
                                <AlertDialog open={isAlertDialogOpen} onOpenChange={setAlertDialogOpen}>
                                    <AlertDialogContent>
                                        <AlertDialogTitle>{t("general.confirm")}</AlertDialogTitle>
                                        <AlertDialogDescription>
                                            {t("resetPasswordPage.resetPassword")}
                                        </AlertDialogDescription>
                                        <AlertDialogAction onClick={handleDialogAction}>
                                            {t("general.ok")}
                                        </AlertDialogAction>
                                        <AlertDialogCancel>{t("general.cancel")}</AlertDialogCancel>
                                    </AlertDialogContent>
                                </AlertDialog>
                            </div>
                        )}
                        {activeForm === 'Personal Info' && (
                            <div>
                                <Card className="mx-10 w-auto">
                                    <CardContent>
                                        <Form {...formUserData}>
                                            {// @ts-expect-error - fix this maybe
                                                <form onSubmit={formUserData.handleSubmit(onSubmitUserData)}
                                                      className="space-y-4">
                                                    <div className="grid gap-4 p-5">
                                                        <div className="grid gap-2">
                                                            <FormField
                                                                control={formUserData.control}
                                                                name="name"
                                                                render={({field}) => (
                                                                    <FormItem>
                                                                        <FormLabel
                                                                            className="text-black">{t("accountSettings.users.table.settings.account.personalInfo.firstName")} *</FormLabel>
                                                                        <FormControl>
                                                                            <Input
                                                                                placeholder={managedUser?.name} {...field}/>
                                                                        </FormControl>
                                                                        <FormMessage/>
                                                                    </FormItem>
                                                                )}
                                                            />
                                                        </div>
                                                        <div className="grid gap-2">
                                                            <FormField
                                                                control={formUserData.control}
                                                                name="lastName"
                                                                render={({field}) => (
                                                                    <FormItem>
                                                                        <FormLabel
                                                                            className="text-black">{t("accountSettings.users.table.settings.account.personalInfo.lastName")} *</FormLabel>
                                                                        <FormControl>
                                                                            <Input
                                                                                placeholder={managedUser?.lastname} {...field}/>
                                                                        </FormControl>
                                                                        <FormMessage/>
                                                                    </FormItem>
                                                                )}
                                                            />
                                                        </div>
                                                        <div className="grid gap-2">
                                                            <FormField
                                                                control={formUserData.control}
                                                                name="phoneNumber"
                                                                render={() => (
                                                                    <FormItem className="items-start">
                                                                        <FormLabel
                                                                            className="text-black text-center">{t("registerPage.phoneNumber")} *</FormLabel>
                                                                        <FormControl className="w-full">
                                                                            <Controller
                                                                                name="phoneNumber"
                                                                                control={formUserData.control}
                                                                                render={({field}) => (
                                                                                    <PhoneInput
                                                                                        {...field}
                                                                                        value={field.value || ""}
                                                                                        placeholder={managedUser?.phoneNumber}
                                                                                        onChange={field.onChange}
                                                                                        countries={['PL']}
                                                                                        defaultCountry="PL"
                                                                                    />
                                                                                )}
                                                                            />
                                                                        </FormControl>
                                                                        <FormMessage/>
                                                                    </FormItem>
                                                                )}
                                                            />
                                                        </div>
                                                        <div className="grid gap-2">
                                                            <FormField
                                                                control={formUserData.control}
                                                                name="twoFactorAuth"
                                                                render={() => (
                                                                    <FormField
                                                                        control={formUserData.control}
                                                                        name="twoFactorAuth"
                                                                        render={({field}) => (
                                                                            <FormItem>
                                                                                <div className="flex flex-col">
                                                                                    <FormLabel className="text-black">
                                                                                        {t("accountSettings.twoFactorAuth")}
                                                                                    </FormLabel>
                                                                                    <FormControl>
                                                                                        <div
                                                                                            className={"justify-center pt-5"}>
                                                                                            <Switch {...field}
                                                                                                    defaultChecked={managedUser?.twoFactorAuth}
                                                                                                    checked={field.value}
                                                                                                    onCheckedChange={field.onChange}
                                                                                            />
                                                                                        </div>
                                                                                    </FormControl>
                                                                                </div>
                                                                                <FormMessage/>
                                                                            </FormItem>
                                                                        )}
                                                                    />
                                                                )}
                                                            />
                                                        </div>
                                                        <Button type="submit" className="w-full pb-2"
                                                                disabled={isLoading}>
                                                            {isLoading ? (
                                                                <>
                                                                    <Loader2 className="mr-2 h-4 w-4 animate-spin"/>
                                                                </>
                                                            ) : (
                                                                t("accountSettings.users.table.settings.account.personalInfo.saveChanges")
                                                            )}
                                                        </Button>
                                                    </div>
                                                </form>
                                            }
                                        </Form>
                                    </CardContent>
                                </Card>
                                <AlertDialog open={isAlertDialogOpen} onOpenChange={setAlertDialogOpen}>
                                    <AlertDialogContent>
                                        <AlertDialogTitle>{t("general.confirm")}</AlertDialogTitle>
                                        <AlertDialogDescription>
                                            {t("accountSettings.confirmUserDataChange")}
                                        </AlertDialogDescription>
                                        <AlertDialogAction onClick={handleDialogAction}>
                                            {t("general.ok")}
                                        </AlertDialogAction>
                                        <AlertDialogCancel>{t("general.cancel")}</AlertDialogCancel>
                                    </AlertDialogContent>
                                </AlertDialog>
                            </div>
                        )}
                        {activeForm === 'Details' && (
                            <div>
                                <Card className="mx-10 w-auto text-left">
                                    <CardContent>
                                        <h2 className="text-lg font-bold text-center p-5">{t("accountSettings.accountInfo")}</h2>
                                        <p>
                                            <strong>{t("accountSettings.name")}:</strong> {managedUser?.name} {managedUser?.lastname}
                                        </p>
                                        <p><strong>{t("accountSettings.email")}:</strong> {managedUser?.email}</p>
                                        <p><strong>{t("accountSettings.login")}:</strong> {managedUser?.login}</p>
                                        <p><strong>{t("accountSettings.phone")}:</strong> {managedUser?.phoneNumber}</p>
                                        <p>
                                            <strong>{t("accountSettings.accountLanguage")}: </strong>
                                            {managedUser?.accountLanguage?.toLowerCase() === 'en' ? t("general.english") :
                                                managedUser?.accountLanguage?.toLowerCase() === 'pl' ? t("general.polish") :
                                                    managedUser?.accountLanguage}
                                        </p>
                                        <p>
                                            <strong>{t("accountSettings.active")}:</strong> {managedUser?.active ?
                                            <FiCheck color="green"/> : <FiX color="red"/>}
                                        </p>
                                        <p>
                                            <strong>{t("accountSettings.blocked")}:</strong> {managedUser?.blocked ?
                                            <FiCheck color="green"/> : <FiX color="red"/>}
                                        </p>
                                        <p>
                                            <strong>{t("accountSettings.verified")}:</strong> {managedUser?.verified ?
                                            <FiCheck color="green"/> : <FiX color="red"/>}
                                        </p>
                                        <p>
                                            <strong>{t("accountSettings.2fa")}:</strong> {managedUser?.twoFactorAuth ?
                                            <FiCheck color="green"/> : <FiX color="red"/>}
                                        </p>
                                        <p>
                                            <strong>{t("accountSettings.lastSucLoginTime")}:</strong> {managedUser?.lastSuccessfulLoginTime ? managedUser.lastSuccessfulLoginTime.toDateString() : 'N/A'}
                                        </p>
                                        <p>
                                            <strong>{t("accountSettings.lastUnsucLoginTime")}:</strong> {managedUser?.lastUnsuccessfulLoginTime ? managedUser.lastUnsuccessfulLoginTime.toDateString() : 'N/A'}
                                        </p>
                                        <p>
                                            <strong>{t("accountSettings.lastSucLoginIp")}:</strong> {managedUser?.lastSuccessfulLoginIp || 'N/A'}
                                        </p>
                                        <p>
                                            <strong>{t("accountSettings.lastUnsucLoginIp")}:</strong> {managedUser?.lastUnsuccessfulLoginIp || 'N/A'}
                                        </p>
                                    </CardContent>
                                </Card>
                            </div>
                        )}
                    </div>
                </div>
            </main>
        </div>
    );
}

export default UserAccountSettings;