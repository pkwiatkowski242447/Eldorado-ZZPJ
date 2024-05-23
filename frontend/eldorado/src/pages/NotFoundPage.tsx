import { Link } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useTranslation } from "react-i18next";

const NotFoundPage = () => {
    const { t } = useTranslation();

    return (
        <div className="flex flex-col items-center justify-center h-screen">
            <h1 className="text-3xl font-bold mb-4">{t("notFound.title")}</h1>
            <p className="text-lg mb-8">{t("notFound.description")}</p>
            <Link to="/" className="mb-4">
                <Button>{t("notFound.home")}</Button>
            </Link>
        </div>
    );
};

export default NotFoundPage;
