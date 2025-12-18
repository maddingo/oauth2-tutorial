'use client';

import { useAuth } from '@/context/AuthContext';
import {getQuote} from "@/lib/quote-api";
import {useEffect, useState} from "react";

export default function Quote() {
    const { accessToken } = useAuth();
    const [quote, setQuote] = useState<{quote: string, author: string} | null>(null);

    useEffect(() => {
        if (accessToken) {
            getQuote(accessToken).then(setQuote).catch(console.error);
        }
    }, [accessToken]);

    if (!quote) {
        return <div>Loading quote...</div>;
    }

    return (
            <div>
                <div className="quote">{quote.quote}</div>
                <div className="author">{quote.author}</div>
            </div>
    );
}
