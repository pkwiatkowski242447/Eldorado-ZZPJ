import { useEffect, useState } from 'react';
import {jwtDecode} from "jwt-decode";
import { api } from "@/api/api.ts";
import {
    AlertDialog,
    AlertDialogAction, AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogTitle
} from "@/components/ui/alert-dialog.tsx";
import { useTranslation } from "react-i18next";
import {useAccount} from "@/hooks/useAccount.ts";
import {toast} from "@/components/ui/use-toast.ts";
import handleApiError from "@/components/HandleApiError.ts";

const SessionHandler = () => {
    const [isRefreshDialogOpen, setRefreshDialogOpen] = useState(false);
    const [isExpiredDialogOpen, setExpiredDialogOpen] = useState(false);
    const { t } = useTranslation();
    const {logOut} = useAccount();

    const checkTokenExpiration = () => {
        const token = localStorage.getItem('token');
        if (token) {
            const decodedToken = jwtDecode(token);
            const currentTime = Date.now() / 1000;
            const expiryTime = decodedToken.exp;
            if (expiryTime) {
                if (currentTime > expiryTime) {
                    setExpiredDialogOpen(true);
                } else {
                    const timeoutDuration = (expiryTime - currentTime - 60) * 1000; //1 minute before expiration
                    const timeoutId = setTimeout(() => {
                        setRefreshDialogOpen(true);
                    }, timeoutDuration);
                    return () => clearTimeout(timeoutId);
                }
            }
        }
    };

    const handleRefreshSession = () => {
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
            api.refreshSession(refreshToken)
                .then(response => {
                    localStorage.setItem('token', response.data.accessToken);
                    localStorage.setItem('refreshToken', response.data.refreshToken);
                    toast({
                        title: t("general.refreshSession.popUp1"),
                        description: t("general.refreshSession.popUp2")
                    })
                }).catch((error) => {
                handleApiError(error);
            });
        }
        setRefreshDialogOpen(false);
    };

    const handleLogout = () => {
        logOut();
    };

    useEffect(checkTokenExpiration, []);

    return (
        <>
            <AlertDialog open={isRefreshDialogOpen} onOpenChange={setRefreshDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogTitle>{t("general.sessionAboutToExpire")}</AlertDialogTitle>
                    <AlertDialogDescription>
                        {t("general.sessionAboutToExpire.text")}
                    </AlertDialogDescription>
                    <AlertDialogAction onClick={handleRefreshSession}>{t("general.refreshSession")}</AlertDialogAction>
                    <AlertDialogCancel>{t("general.cancel")}</AlertDialogCancel>
                </AlertDialogContent>
            </AlertDialog>
            <AlertDialog open={isExpiredDialogOpen} onOpenChange={setExpiredDialogOpen}>
                <AlertDialogContent>
                    <AlertDialogTitle>{t("general.sessionExpired")}</AlertDialogTitle>
                    <AlertDialogDescription>
                        {t("general.sessionExpired.text")}
                    </AlertDialogDescription>
                    <AlertDialogAction onClick={handleLogout}>{t("general.sessionExpired.text.button")}</AlertDialogAction>
                </AlertDialogContent>
            </AlertDialog>
        </>
    );
};

export default SessionHandler;